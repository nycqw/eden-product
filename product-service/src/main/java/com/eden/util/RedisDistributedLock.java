package com.eden.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分布式锁需要满足一下四个条件：
 * 1、互斥性。在任意时刻，只有一个客户端能持有锁。
 * 2、不会发生死锁。即使有一个客户端在持有锁的期间崩溃而没有主动解锁，也能保证后续其他客户端能加锁。
 * 3、具有容错性。只要大部分的Redis节点正常运行，客户端就可以加锁和解锁。
 * 4、解铃还须系铃人。加锁和解锁必须是同一个客户端，客户端自己不能把别人加的锁给解了。
 *
 * @author chenqw
 * @version 1.0
 * @since 2018/12/1
 */
@Component
@Slf4j
public class RedisDistributedLock {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 加锁
     *
     * @param lockName   锁名称
     * @param identifier 锁标记
     * @return
     */
    public boolean lock(String lockName, String identifier, Long expire) {
        RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
        RedisConnection connection = connectionFactory.getConnection();
        while (true) {
            if (tryLock(identifier, connectionFactory, connection, lockName, expire))
                return true;
        }
    }

    /**
     * 可设置超时时间的锁
     *
     * @param lockName   锁名称
     * @param identifier 锁标记
     * @param timeout    获取锁的超时时间
     * @param expire     超时时间（毫秒）
     * @return 是否成功
     */
    public boolean lockWithTimeout(String lockName, String identifier, long timeout, long expire) {
        RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
        RedisConnection connection = connectionFactory.getConnection();
        long end = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < end) {
            if (tryLock(identifier, connectionFactory, connection, lockName, expire))
                return true;
        }
        RedisConnectionUtils.releaseConnection(connection, connectionFactory);
        return false;
    }

    /**
     * 尝试获取锁
     */
    private boolean tryLock(String identifier, RedisConnectionFactory connectionFactory, RedisConnection connection, String lockName, Long expire) {
        int lockExpire = (int) (expire / 1000);
        // 1、只有一个客户端能获取到锁
        if (connection.setNX(lockName.getBytes(), identifier.getBytes())) {
            // 2.1、若在这里程序突然崩溃，则无法设置过期时间，将发生死锁
            connection.expire(lockName.getBytes(), lockExpire);
            RedisConnectionUtils.releaseConnection(connection, connectionFactory);
            log.info("=================获取锁成功================={}", Thread.currentThread().getName());
            return true;
        }

        // 2.2、解决2.1处可能出现的死锁问题
        // 当出现锁没有设置过期时间时由其他线程来为其设置过期时间
        if (connection.ttl(lockName.getBytes()) == -1) {
            connection.expire(lockName.getBytes(), lockExpire);
        }

        // 延迟100毫秒后再次尝试获取锁
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            log.warn("获取到分布式锁：线程中断！");
            Thread.currentThread().interrupt();
        }
        return false;
    }

    /**
     * 释放锁
     *
     * @param lockName   锁的key
     * @param identifier 释放锁的标识
     * @return 是否成功
     */
    public boolean unLock(String lockName, String identifier) {
        if (identifier == null || "".equals(identifier)) {
            return false;
        }
        RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
        RedisConnection connection = connectionFactory.getConnection();
        boolean releaseFlag = false;
        while (true) {
            try {
                // 监视lock，准备开始事务
                connection.watch(lockName.getBytes());
                byte[] valueBytes = connection.get(lockName.getBytes());
                // 未占有锁无法释放
                if (valueBytes == null) {
                    connection.unwatch();
                    releaseFlag = false;
                    break;
                }
                // 4、加锁和解锁必须是同一个客户端
                if (identifier.equals(new String(valueBytes))) {
                    connection.multi();
                    // 释放锁
                    connection.del(lockName.getBytes());
                    List<Object> results = connection.exec();
                    if (results == null) {
                        continue;
                    }
                    log.info("=================释放锁成功================={}", Thread.currentThread().getName());
                    releaseFlag = true;
                }
                connection.unwatch();
                break;
            } catch (Exception e) {
                log.warn("释放锁异常", e);
            }
        }
        RedisConnectionUtils.releaseConnection(connection, connectionFactory);
        return releaseFlag;
    }
}
