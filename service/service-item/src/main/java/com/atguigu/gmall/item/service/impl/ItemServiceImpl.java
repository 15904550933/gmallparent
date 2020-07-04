package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeginClient;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * @author smy
 * @create 2020-06-13 11:23
 */
@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;

    //编写一个自定义的线程池
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ListFeginClient listFeginClient;


    @Override
    public Map<String, Object> getBySkuId(Long skuId) {
        HashMap<String, Object> result = new HashMap<>();
        /*
         * 1.Sku基本信息
         *   result.put("1","Sku基本信息")
         * 2.Sku图片信息
         *   result.put("2","Sku图片信息")
         * 3.Sku分类信息
         *   result.put("3","Sku分类信息")
         * 4.Sku销售属性相关信息
         *   result.put("4","Sku销售属性相关信息")
         * 5.Sku价格信息
         *   result.put("5","Sku价格信息")
         */

        //异步编排
        //通过skuId获取skuInfo 对象数据
        //因为后续 需要 使用skuInfo中的属性，所以选用带返回值的方法 supplyAsync()
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfoById(skuId);
            //保存skuInfo
            result.put("skuInfo", skuInfo);
            return skuInfo;
        },threadPoolExecutor);


        //通过skuId和spuID获取销售属性集合数据
        //因为后续不需要使用其中的属性，只保存即可。不需要这个集合数据作为参数传递
        //销售属性和属性值需要 skuInfo对象中的spuId 所以此处应该使用 skuInfoCompletableFuture
        //因为使用上一个结果，只能选择 串行or并行 ，因不需要返回结果，所以选择 thenAccept（）
        CompletableFuture<Void> spuSaleAttrCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo -> {
            List<SpuSaleAttr> spuSaleAttrListCheckBySku = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
            // 保存销售属性-销售属性值集合数据
            result.put("spuSaleAttrList", spuSaleAttrListCheckBySku);
        }),threadPoolExecutor);


        //通过三级分类ID获取分类数据
        //分类数据需要skuInfo的三级分类Id
        CompletableFuture<Void> categoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo) -> {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            // 保存三级分类数据
            result.put("categoryView", categoryView);
        },threadPoolExecutor);


        //通过skuID获取价格信息
        //异步编排 以及 使用 skuInfoCompletableFuture 均可
        //因为后续 不需要 使用价格信息中的属性，所以选用带返回值的方法 runAsync()
        //方法一：异步编排
        CompletableFuture<Void> getSkuPriceCompletableFuture = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            // 保存价格
            result.put("price", skuPrice);
        },threadPoolExecutor);
        /*
        //方法二：使用 skuInfoCompletableFuture
        CompletableFuture<Void> getSkuPriceCompletableFuture = skuInfoCompletableFuture.thenAccept((skuInfo) -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            // 保存价格
            result.put("price", skuPrice);
        });
        */


        //通过skuID获取由销售属性值ID和skuId组成的map集合数据 {"30","50|52"}
        CompletableFuture<Void> valuesSkuJsonCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo -> {
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
//             需要将skuValueIdsMap 转化为Json 字符串，给页面使用!  Map --->Json
            String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
            // 保存销售属性值Id 和 skuId 组成的json 字符串
            result.put("valuesSkuJson",valuesSkuJson);
        }),threadPoolExecutor);

        //热度排名计算
        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.runAsync(() -> {
            //调用热度排名方法
            listFeginClient.incrHotScore(skuId);
        }, threadPoolExecutor);

        //将所有的异步编排做整合
        CompletableFuture.allOf(skuInfoCompletableFuture,
                                spuSaleAttrCompletableFuture,
                                categoryViewCompletableFuture,
                                getSkuPriceCompletableFuture,
                                valuesSkuJsonCompletableFuture,
                                voidCompletableFuture).join();

//        //通过skuId获取sku信息
//        SkuInfo skuInfo = productFeignClient.getSkuInfoById(skuId);
//        //通过skuId和spuID获取销售属性集合数据
//        List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
//        //通过三级分类ID获取分类数据
//        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
//        //通过skuID获取价格信息
//        BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
//        //通过skuID获取由销售属性值ID和skuId组成的map集合数据 {"30","50|52"}
//        Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
//
         /*保存数据：这个位置需要根据前端结合一块使用  key=""是谁{key 由商品详情页面决定的，${skuInfo}}*/




        return result;
    }
}
