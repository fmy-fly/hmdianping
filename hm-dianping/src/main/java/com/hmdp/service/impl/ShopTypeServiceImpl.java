package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList()
    {

        String key = RedisConstants.CACHE_SHOP_TYPE_LIST;
        // 1. 从redis中查询店铺类型缓存  end:-1 表示取全部数据
        List<String> shopTypeJson = stringRedisTemplate.opsForList().range(key, 0, -1);
        // 2. 判断是否存在
        if (shopTypeJson!= null && !shopTypeJson.isEmpty()) {
            // 存在,直接返回
            List<ShopType> shopTypeList = shopTypeJson.stream()
                    .map(json -> JSONUtil.toBean(json, ShopType.class)).sorted(Comparator.comparingInt(ShopType::getSort)).collect(Collectors.toList());
            return Result.ok(shopTypeList);
        }
        // 3. 不存在，查询数据库列表
        List<ShopType> shopTypeList = query().orderByAsc("sort").list();
        // 4. 数据库中不存在，返回错误
        if (shopTypeList == null || shopTypeList.isEmpty()) {
            return Result.fail("查询异常，商铺类型不存在。。。");
        }

        // 5. 存在,写入redis，这里使用Redis,每个元素都要单独转成JSON，使用stream流的map映射
        List<String> shopTypeJsonList = shopTypeList.stream()
                .map(JSONUtil::toJsonStr)
                .collect(Collectors.toList());
        // 6. 写入 Redis（使用 rightPush 保证顺序）
        stringRedisTemplate.opsForList().rightPushAll(key, shopTypeJsonList);
        // 7.返回结果
        return Result.ok(shopTypeList);
    }
}
