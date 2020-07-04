package com.atguigu.gmall.item.service;

import java.util.Map;

/**
 * @author zm
 * @create 2020-06-13 11:22
 */
public interface ItemService {
    /**
     * 获取sku详情信息
     * @param skuId
     * @return
     */
    Map<String, Object> getBySkuId(Long skuId);
}
