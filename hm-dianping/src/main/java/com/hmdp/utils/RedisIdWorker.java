package com.hmdp.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @ClassName：RedisIdWorker
 * @Author: fmy
 * @Date: 2025/1/10 17:56
 * @Description:
 */
@Component
public class RedisIdWorker {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /*
     * s开始时间戳
     */
    private static final long BEGIN_TIMESTAMP = 1640995200;
    /*
        移数
     */
    private static final int COUNT_BITS = 32;


    public long nextId(String keyPrefix) {
        // 1. 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = nowSecond - BEGIN_TIMESTAMP;
        // 2. 生成序列号
        // 2.1 获取当前日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        // 2.2 自增长
        long count = stringRedisTemplate.opsForValue().increment("irc:" + keyPrefix + ":" + date);
        // 3. 拼接并返回
        return timeStamp << COUNT_BITS | count ;
    }

}
