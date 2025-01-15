package com.hmdp;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName：RedissonTest
 * @Author: fmy
 * @Date: 2025/1/15 14:31
 * @Description: redisson 多redis节点测试
 */
@Slf4j
@SpringBootTest
public class RedissonTest {
    @Autowired
    @Qualifier("redissonClient")
    private RedissonClient redissonclient;
    @Autowired
    @Qualifier("redissonClient2")
    private RedissonClient redissonclient2;


    private RLock lock;

    @BeforeEach
    void setUp() {
        RLock lock1 = redissonclient.getLock("order");
        RLock lock2 = redissonclient2.getLock("order");

        // 创建联锁 multiLock
        lock = redissonclient.getMultiLock(lock1, lock2);
    }

    @Test
    void method1() throws InterruptedException {
        // 尝试获取锁
        boolean isLock = lock.tryLock(1L, TimeUnit.SECONDS);
        if (!isLock) {
            log.error("获取锁失败 .... 1");
            return;
        }
        try {
            log.info("获取锁成功 .... 1");
            method2();
            log.info("开始执行业务 ... 1");
        } finally {
            log.warn("准备释放锁 .... 1");
            lock.unlock();
        }
    }

    void method2() {
        // 尝试获取锁
        boolean isLock = lock.tryLock();
        if (!isLock) {
            log.error("获取锁失败 .... 2");
            return;
        }
        try {
            log.info("获取锁成功 .... 2");
            log.info("开始执行业务 ... 2");
        } finally {
            log.warn("准备释放锁 .... 2");
            lock.unlock();


        }
    }
}
