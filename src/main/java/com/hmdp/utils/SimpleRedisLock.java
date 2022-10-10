package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 简单的redis分布式锁
 *
 * @author CHEN
 * @date 2022/10/09
 */
public class SimpleRedisLock implements ILock {
    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final String KET_PREFIX="lock:";
    private static final String ID_PREFIX= UUID.randomUUID().toString(true)+"-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(Long timeoutSec) {
        String threadId =ID_PREFIX+ Thread.currentThread().getId();
        //获取锁
        Boolean isSuccess = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(KET_PREFIX + name
                        , threadId
                        , timeoutSec
                        , TimeUnit.SECONDS);
        //避免自动拆箱引发空指针异常
        return Boolean.TRUE.equals(isSuccess);
    }

    @Override
    public void unLock() {
        /*//获取线程标识
        String threadId =ID_PREFIX+ Thread.currentThread().getId();
        //获取锁中标识
        String id = stringRedisTemplate.opsForValue().get(KET_PREFIX + name);
        //判断时候一致
        if (StringUtils.equals(id,threadId)){
            //一致 释放锁
            stringRedisTemplate.delete(KET_PREFIX + name);
        }*/
        //使用lua脚本保证操作原子性
        stringRedisTemplate.execute(UNLOCK_SCRIPT
                , Collections.singletonList(KET_PREFIX+name)
                ,ID_PREFIX+Thread.currentThread().getId());
    }
}
