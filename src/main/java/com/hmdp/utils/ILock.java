package com.hmdp.utils;

/**
 * redis分布式锁
 *
 * @author CHEN
 * @date 2022/10/09
 */
public interface ILock {
    /**
     * 尝试获取锁
     *
     * @param timeoutSec 超时自动释放锁
     * @return 是否成功获取锁 true成功 false失败
     */
    boolean tryLock(Long timeoutSec);

    /**
     * 释放锁
     */
    void unLock();
}
