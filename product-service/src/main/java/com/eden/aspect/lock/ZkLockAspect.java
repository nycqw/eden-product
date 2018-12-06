package com.eden.aspect.lock;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @author chenqw
 * @since 2018/12/5
 */
@Component
@Aspect
@Slf4j
public class ZkLockAspect {

    @Autowired
    private CuratorFramework curatorFramework;

    private InterProcessMutex lock;

    @PostConstruct
    private void init() {
        lock = new InterProcessMutex(curatorFramework, "/zklock");
    }

    @Pointcut(value = "@annotation(com.eden.aspect.lock.annotation.ZkLock)")
    public void pointCut() {
    }

    @Around(value = "pointCut()")
    public Object addLock(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            lock.acquire();
            log.info("========获取锁成功======={}", Thread.currentThread().getName());
            return joinPoint.proceed();
        } catch (Exception e) {
            log.info("获取锁失败->{}", e);
        } finally {
            try {
                lock.release();
                log.info("========释放锁成功======={}", Thread.currentThread().getName());
            } catch (Exception e) {
                log.error("释放锁失败");
            }
        }
        return null;
    }
}
