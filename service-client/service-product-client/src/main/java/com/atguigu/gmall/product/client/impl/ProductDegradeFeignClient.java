package com.atguigu.gmall.product.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author zm
 * @create 2020-06-15 9:10
 */
@Component
public class ProductDegradeFeignClient implements ProductFeignClient {

    /**
     * 根据skuId获取sku信息
     * @param skuId
     * @return
     */
    @Override
    public SkuInfo getSkuInfoById(Long skuId) {
        return null;
    }

    /**
     * 通过三级分类id查询分类信息
     * @param category3Id
     * @return
     */
    @Override
    public BaseCategoryView getCategoryView(Long category3Id) {
        return null;
    }

    /**
     * 获取sku最新价格
     * @param skuId
     * @return
     */
    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        return null;
    }

    /**
     * 根据spuId，skuId 查询销售属性集合
     * @param skuId
     * @param spuId
     * @return
     */
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        return null;
    }

    /**
     * 根据spuId 查询map 集合属性
     * 场景：点击销售属性值（某个颜色或配置）进行切换
     * @param spuId
     * @return
     */
    @Override
    public Map getSkuValueIdsMap(Long spuId) {
        return null;
    }


    /**
     * 获取首页全部分类信息
     * @return
     */
    @Override
    public Result getBaseCategoryList() {
        return null;
    }


    /**
     * 品牌信息  base_trademark
     * @param tmId 品牌Id
     * @return
     */
    @Override
    public BaseTrademark getTrademark(Long tmId) {
        return null;
    }

    /**
     * 通过skuId 集合来查询数据 获取平台属性，平台属性值
     * @param skuId
     * @return
     */
    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        return null;
    }
}
