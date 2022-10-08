package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        //缓存穿透
//        Shop shop = queryWithPassThrough(id);
//        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);
        //逻辑过期解决缓存击穿
//        Shop shop = queryWithLogicalExpire(id);
        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    /**
     * 互斥锁解决缓存击穿
     *
     * @param id id
     * @return {@link Shop}
     */
    /*private Shop queryWithMutex(Long id) {
        String shopKey = CACHE_SHOP_KEY + id;
        //从redis中查询
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        //判断是否存在
        if (StringUtils.isNotEmpty(shopJson)) {
            //存在直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //判断控值
        if ("".equals(shopJson)) {
            return null;
        }
        //实现缓存重建
        //获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            //是否获取成功
            if (!isLock) {
                //获取失败 休眠并且重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //成功 通过id查询数据库
            shop = getById(id);
            //模拟重建延时
            Thread.sleep(200);
            if (shop == null) {
                //redis写入空值
                stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                //数据库不存在 返回错误
                return null;
            }
            //数据库存在 写入redis
            stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //释放互斥锁
            unLock(lockKey);
        }
        //返回
        return shop;
    }*/

    /**
     * 逻辑过期解决缓存击穿
     *
     * @param id id
     * @return {@link Shop}
     */
    /*private Shop queryWithLogicalExpire(Long id) {
        String shopKey = CACHE_SHOP_KEY + id;
        //从redis中查询
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        //判断是否存在
        if (StringUtils.isEmpty(shopJson)) {
            //不存在返回空
            return null;
        }
        //命中 反序列化
        RedisDate redisDate = JSONUtil.toBean(shopJson, RedisDate.class);
        JSONObject jsonObject = (JSONObject) redisDate.getData();
        Shop shop = BeanUtil.toBean(jsonObject, Shop.class);
        LocalDateTime expireTime = redisDate.getExpireTime();
        //判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            //未过期 直接返回
            return shop;
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
                    this.saveShopToRedis(id, 20L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unLock(lockKey);
                }
            });
        }
        //返回过期商铺信息
        return shop;
    }*/

    /**
     * 使用设置空值解决缓存穿透
     *
     */
   /* private Shop queryWithPassThrough(Long id) {
        String shopKey = CACHE_SHOP_KEY + id;
        //从redis中查询
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        //判断是否存在
        if (StringUtils.isNotEmpty(shopJson)) {
            //存在直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //判断空值
        if ("".equals(shopJson)) {
            return null;
        }
        //不存在 查询数据库
        Shop shop = getById(id);
        if (shop == null) {
            //redis写入空值
            stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            //数据库不存在 返回错误
            return null;
        }
        //数据库存在 写入redis
        stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //返回
        return shop;
    }*/

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("id不能为空");
        }
        //更新数据库
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }

//    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

/*    *//**
     * 获取锁
     *
     * @param key 关键
     * @return boolean
     *//*
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    *//**
     * 释放锁
     *
     * @param key 关键
     *//*
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }

    *//**
     * 存入redis 携带逻辑过期时间
     *//*
    public void saveShopToRedis(Long id, Long expireSeconds) throws InterruptedException {
        //查询店铺数据
        Shop shop = getById(id);
        Thread.sleep(200);
        //封装逻辑过期
        RedisDate redisDate = new RedisDate();
        redisDate.setData(shop);
        redisDate.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //写了redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisDate));
    }*/
}
