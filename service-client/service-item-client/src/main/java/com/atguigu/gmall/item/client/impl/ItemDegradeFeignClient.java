package com.atguigu.gmall.item.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.stereotype.Component;

/**
 * @author smy
 * @create 2020-06-15 10:21
 */
@Component
public class ItemDegradeFeignClient implements ItemFeignClient {

    /**
     * 根据skuID获取商品详情的数据
     * @param skuId
     * @return
     */
    @Override
    public Result getItem(Long skuId) {
        return Result.fail();
    }
}
