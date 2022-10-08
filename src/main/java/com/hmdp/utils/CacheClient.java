package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * redis工具
 *
 * @author CHEN
 * @date 2022/10/08
 */
@Slf4j
@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 将任意对象序列化成json存入redis
     *
     * @param key   关键
     * @param value 价值
     * @param time  时间
     * @param unit  单位
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 将任意对象序列化成json存入redis 并且携带逻辑过期时间
     *
     * @param key   关键
     * @param value 价值
     * @param time  时间
     * @param unit  单位
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        //封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        //存入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 设置空值解决缓存穿透
     *
     * @param keyPrefix  关键前缀
     * @param id         id
     * @param type       类型
     * @param dbFallback db回退
     * @param time       时间
     * @param unit       单位
     * @return {@link R}
     */
    public <R, ID> R queryWithPassThrough(
            String keyPrefix
            , ID id
            , Class<R> type
            , Function<ID, R> dbFallback
            , Long time
            , TimeUnit unit) {
        String key = keyPrefix + id;
        //从redis中查询
        String json = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StringUtils.isNotEmpty(json)) {
            //存在直接返回
            return JSONUtil.toBean(json, type);
        }
        //判断空值
        if ("".equals(json)) {
            return null;
        }
        //不存在 查询数据库
        R r = dbFallback.apply(id);
        if (r == null) {
            //redis写入空值
            this.set(key, "", CACHE_NULL_TTL, TimeUnit.SECONDS);
            //数据库不存在 返回错误
            return null;
        }
        //数据库存在 写入redis
        this.set(key, r, time, unit);
        //返回
        return r;
    }

    /**
     * 逻辑过期解决缓存击穿
     *
     * @param id id
     * @return {@link Shop}
     */
    public <R, ID> R queryWithLogicalExpire(String keyPrefix
            , ID id
            , Class<R> type
            , Function<ID, R> dbFallback
            , Long time
            , TimeUnit unit) {
        String key = keyPrefix + id;
        //从redis中查询
        String json = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StringUtils.isEmpty(json)) {
            //不存在返回空
            return null;
        }
        //命中 反序列化
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        JSONObject jsonObject = (JSONObject) redisData.getData();
        R r = BeanUtil.toBean(jsonObject, type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            //未过期 直接返回
            return r;
        }
        //已过期
        //获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean flag = tryLock(lockKey);
        //是否获取锁成功
        if (flag) {
            //成功 异步重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    //查询数据库
                    R newR = dbFallback.apply(id);
                    //写入redis
                    this.setWithLogicalExpire(key,newR,time,unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unLock(lockKey);
                }
            });
        }
        //返回过期商铺信息
        return r;
    }

    /**
     * 简易线程池
     */
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 获取锁
     *
     * @param key 关键
     * @return boolean
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     *
     * @param key 关键
     */
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }
}
