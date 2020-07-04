package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseAttrVo;
import com.atguigu.gmall.model.list.SearchResponseVo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.PrivateKey;
import java.util.PrimitiveIterator;

/**
 * @author smy
 * @BelongsProject: gmallparent
 * @BelongsPackage: com.atguigu.gmall.list
 * @create 2020-06-19 11:46
 * @Description:
 * service-list 通过 openFegin 调用 service-product-client 从 service-product（数据提供者） 获取数据
 *
 */
public interface SearchService {


    /**
     * 上架商品列表
     * @param skuId
     */
    void upperGoods(Long skuId);

    /**
     * 上架多个商品列表
     */
    void upperGoods();
    void upperGoods(Long... skuId);


    /**
     * 下架商品列表
     * @param skuId
     */
    void lowerGoods(Long skuId);


    /**
     * 更新热点
     * @param skuId
     */
    void incrHotScore(Long skuId);

    /**
     * 检索数据
     * @param searchParam
     * @return
     */
    SearchResponseVo search(SearchParam searchParam) throws Exception;

}
