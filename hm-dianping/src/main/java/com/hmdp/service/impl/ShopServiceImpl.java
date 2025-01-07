package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

/**
 * <p>
 *  服务实现类
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
        // 设置“”值解决缓存穿透
        // Shop shop = queryWithPassThrough(id);

        // 互斥锁解决缓存击穿
        // Shop shop = queryWithMutex(id);

        // 逻辑过期解决缓存击穿
        Shop shop = cacheClient.queryWithLogicExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById,RedisConstants.LOCK_SHOP_TTL,TimeUnit.SECONDS);
        if (shop == null) return Result.fail("店铺不存在!");
        return Result.ok(shop);
    }
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool( 10);
//    private boolean tryLock(String key) {
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//        return BooleanUtil.isTrue(flag);
//    }
//    private void unlock(String key) {
//        stringRedisTemplate.delete(key);
//    }
//    public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
//        // 1. 查询店铺数据
//        Shop shop = getById(id);
//        Thread.sleep(200);
//        // 2. 封装逻辑过期时间
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//
//        // 3. 写入Redis
//        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
//
//    }

//    public Shop queryWithMutex(Long id) {
//        // 1.从Redis查询商铺缓存
//        String key = RedisConstants.CACHE_SHOP_KEY + id;
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        // 2.判断是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            // 3.存在，直接返回
//            return JSONUtil.toBean(shopJson, Shop.class);
//
//        }
//        //判断命中的值是否是空值
//        if (shopJson != null) {
//            // 返回一个错误信息
//            return null;
//        }
//
//        //4. 实现缓存重建
//        //4.1 获取互斥锁
//        String lockKey = LOCK_SHOP_KEY + id;
//        Shop shop = null;
//        try {
//            boolean isLock = tryLock(lockKey);
//
//
//            //4.2 判断是否获取成功
//            if (!isLock) {
//                //4.3 失败，则休眠并重试
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
//
//            //4.4 成功,再次检测redis,判断缓存是否存在，做DoubleCheck
//            // 如果存在则无需重建缓存
//            shopJson = stringRedisTemplate.opsForValue().get(key);
//            // 判断是否存在
//            if (StrUtil.isNotBlank(shopJson)) {
//                // 存在，直接返回
//                unlock(lockKey);
//                return JSONUtil.toBean(shopJson, Shop.class);
//            }
//
//
//            // 不存在，根据id查询数据库
//            shop = getById(id);
//            // 模拟重建的延时
//            Thread.sleep(200);
//            // 5.不存在， 返回错误  又名**缓存击穿**
//            if (shop == null) {
//                // 将空值写入redis
//                stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
//                // 返回错误信息
//                return null;
//            }
//            // 6.存在，写入redis
//            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        finally {
//            // 7. 释放互斥锁
//            unlock(lockKey);
//        }
//
//        // 8.返回
//        return shop;
//    }



//
//    public Shop queryWithLogicExpire(Long id) {
//        // 1.从Redis查询商铺缓存
//        String key = RedisConstants.CACHE_SHOP_KEY + id;
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        // 2.判断是否存在
//        if (StrUtil.isBlank(shopJson)) {
//            // 3.存在，直接返回
//            return null;
//
//        }
//        // 4. 命中，需要先把json反序列化为对象
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//
//        // 5. 判断是否过期
//        if (expireTime.isAfter(LocalDateTime.now())) {
//            //5.1 未过期， 直接返回店铺信息
//            return shop;
//        }
//        // 5.2 已过期，需要缓存重建
//
//        //6. 缓存重建
//        //6.1 获取互斥锁
//        String lockKey = LOCK_SHOP_KEY + id;
//        boolean isLock = tryLock(lockKey);
//        //6.2 判断是否获取锁成功
//        if (isLock) {
//            // 6.3 成功， 开启独立线程，实现缓存创建
//            // 再次检测缓存是否过期
//
//           shopJson = stringRedisTemplate.opsForValue().get(key);
//            if (StrUtil.isNotBlank(shopJson)) {
//                redisData = JSONUtil.toBean(shopJson, RedisData.class);
//                shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
//                expireTime = redisData.getExpireTime();
//                if (expireTime.isAfter(LocalDateTime.now())) {
//                    // 未过期， 直接返回店铺信息
////                unlock(lockKey);
//                    return shop;
//                }
//
//            }
//
//
//
//            CACHE_REBUILD_EXECUTOR.submit(()->{
//                try {
//                    // 重建缓存
//                    this.saveShop2Redis(id, 20L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//                finally {
//                    // 释放锁
//                    unlock(lockKey);
//                }
//
//            });
//
//
//        }
//
//        return shop;
//    }

//    public Shop queryWithPassThrough(Long id) {
//        // 1.从Redis查询商铺缓存
//        String key = RedisConstants.CACHE_SHOP_KEY + id;
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        // 2.判断是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            // 3.存在，直接返回
//            return JSONUtil.toBean(shopJson, Shop.class);
//
//        }
//        //判断命中的值是否是空值
//        if (shopJson != null) {
//            // 返回一个错误信息
//            return null;
//        }
//
//
//        // 4.不存在，根据id查询数据库
//        Shop shop = getById(id);
//        // 5.不存在， 返回错误  又名**缓存击穿**
//        if (shop == null) {
//            // 将空值写入redis
//            stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//        // 6.存在，写入redis
//        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        // 7.返回
//        return shop;
//    }



    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        // 1.更新数据库
        updateById(shop);
        // 2.删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
        return Result.ok();

    }
}
