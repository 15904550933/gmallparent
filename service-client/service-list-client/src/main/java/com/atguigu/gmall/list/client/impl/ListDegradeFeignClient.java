package com.atguigu.gmall.list.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeginClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.stereotype.Component;

/**
 * @author smy
 * @BelongsProject: gmallparent
 * @BelongsPackage: com.atguigu.gmall.list.client.impl
 * @create 2020-06-19 15:38
 * @Description:
 */
@Component
public class ListDegradeFeignClient implements ListFeginClient {
    @Override
    public Result incrHotScore(Long skuId) {
        return null;
    }

    @Override
    public Result list(SearchParam listParam) {
        return null;
    }

    @Override
    public Result upperGoods(Long skuId) {
        return null;
    }

    @Override
    public Result lowerGoods(Long skuId) {
        return null;
    }
}
