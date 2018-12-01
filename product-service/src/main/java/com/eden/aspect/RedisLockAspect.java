package com.eden.aspect;

import com.eden.aspect.annotation.RedisLock;
import com.eden.util.AopUtil;
import com.eden.util.RedisDistributedLock;
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
public class RedisLockAspect {

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    @Pointcut(value = "@annotation(com.eden.aspect.annotation.RedisLock)")
    public void lockCut() {
    }

    @Around(value = "lockCut()")
    public Object interceptor(ProceedingJoinPoint joinPoint) throws Throwable {
        Method targetMethod = AopUtil.getTargetMethod(joinPoint);
        RedisLock redisLock = targetMethod.getAnnotation(RedisLock.class);
        String lockName = getLockName(joinPoint, targetMethod);
        String identifier = UUID.randomUUID().toString();
        boolean acquire = redisDistributedLock.lock(lockName, identifier, redisLock.expire());
        if (acquire) {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                throw throwable;
            } finally {
                // 释放锁
                redisDistributedLock.unLock(lockName, identifier);
            }
        }
        return false;
    }

    private String getLockName(ProceedingJoinPoint joinPoint, Method targetMethod) {
        return AopUtil.getClassName(joinPoint) + "-" + targetMethod.getName();
    }
}
