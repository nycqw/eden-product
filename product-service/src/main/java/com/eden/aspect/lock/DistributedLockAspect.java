package com.eden.aspect.lock;

import com.eden.aspect.lock.annotation.Lock;
import com.eden.aspect.lock.handle.CuratorDistributedLock;
import com.eden.aspect.lock.handle.LockType;
import com.eden.aspect.lock.handle.RedisDistributedLock;
import com.eden.util.AopUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * @author chenqw
 * @version 1.0
 * @since 2018/12/1
 */
@Aspect
@Component
@Slf4j
public class DistributedLockAspect {

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    @Autowired
    private CuratorDistributedLock curatorDistributedLock;

    @Pointcut(value = "@annotation(com.eden.aspect.lock.annotation.Lock)")
    public void lockCut() {
    }

    @Around(value = "lockCut()")
    public Object interceptor(ProceedingJoinPoint joinPoint) throws Throwable {
        Method targetMethod = AopUtil.getTargetMethod(joinPoint);
        Lock lock = targetMethod.getAnnotation(Lock.class);
        if (LockType.REDIS_LOCK.equals(lock.type())) {
            return handleRedisDistributedLock(joinPoint, lock);
        } else {
            return handleCuratorDistributedLock(joinPoint);
        }
    }

    /**
     * 基于zookeeper的分布式锁
     *
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    private Object handleCuratorDistributedLock(ProceedingJoinPoint joinPoint) throws Throwable {
        String lockName = getLockName(joinPoint);
        curatorDistributedLock.lock(lockName);
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            // 不对业务异常进行处理直接抛出
            throw throwable;
        } finally {
            // 释放锁
            curatorDistributedLock.unlock(lockName);
        }
    }

    /**
     * 基于redis的分布式锁
     */
    private Object handleRedisDistributedLock(ProceedingJoinPoint joinPoint, Lock lock) throws Throwable {
        String lockName = getLockName(joinPoint);
        String identifier = UUID.randomUUID().toString();
        // 加锁
        boolean acquire;
        if (lock.timeout() == -1L) {
            // 普通锁
            acquire = redisDistributedLock.lock(lockName, identifier, lock.expire());
        } else {
            // 设置有超时时间的锁
            acquire = redisDistributedLock.lockWithTimeout(lockName, identifier, lock.expire(), lock.timeout());
        }
        if (acquire) {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                // 不对业务异常进行处理直接抛出
                throw throwable;
            } finally {
                // 释放锁
                redisDistributedLock.unLock(lockName, identifier);
            }
        }
        return null;
    }

    /**
     * 设置锁名称
     */
    private String getLockName(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        Method targetMethod = AopUtil.getTargetMethod(joinPoint);
        return AopUtil.getClassName(joinPoint) + "#" + targetMethod.getName();
    }
}
