package com.eden.aspect.lock;

/**
 * @author
 * @since 2018/12/5
 */
public abstract class AbstractLock implements Lock {

    public abstract boolean lock(Long timeout);

    public abstract boolean tryLock();

    public abstract boolean waitLock();
}
