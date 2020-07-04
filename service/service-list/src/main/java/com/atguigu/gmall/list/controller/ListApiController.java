package com.atguigu.gmall.list.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.list.Goods;


import com.atguigu.gmall.list.service.SearchService ;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import com.baomidou.mybatisplus.extension.api.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * @author smy
 * @BelongsProject: gmallparent
 * @BelongsPackage: com.atguigu.gmall.list.controller
 * @create 2020-06-19 10:42
 * @Description:
 */
@RestController
@RequestMapping("api/list")
public class ListApiController {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private SearchService searchService;

    /**
     * 利用注解
     * 初始化mapping结构到es中
     * @return
     */
    @GetMapping("inner/createIndex")
    public Result createIndex(){
        //创建index，type
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);
        return Result.ok();
    }

    /**
     * 上架商品
     * @param skuId
     * @return
     */
    @GetMapping("inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable("skuId") Long skuId) {
        searchService.upperGoods(skuId);
        return Result.ok();
    }

    /**
     * 下架商品
     * @param skuId
     * @return
     */
    @GetMapping("inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable("skuId") Long skuId) {
        searchService.lowerGoods(skuId);
        return Result.ok();
    }

    /**
     * 商品热度排名
     * @param skuId
     * @return
     */
    @GetMapping("@inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable("skuId") Long skuId){
        searchService.incrHotScore(skuId);
        return Result.ok();
    }

    /**
     * 查询的时候输入JSON字符串
     * @param searchParam
     * @return
     */
    @PostMapping
    public Result getList(@RequestBody SearchParam searchParam){
        SearchResponseVo search = null;
        try {
            search = searchService.search(searchParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.ok(search);
    }
}
