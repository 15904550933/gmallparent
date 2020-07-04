package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author smy
 * @create 2020-06-09 14:39
 */
@RestController
@RequestMapping("admin/product")
public class BaseManageController {

    @Autowired
    private ManageService manageService;

    /**
     * 查询一级分类信息
     * @return
     */
    @GetMapping("getCategory1")
    public Result<List<BaseCategory1>> getCategory1(){
        List<BaseCategory1> category1List = manageService.getCategory1();
        return Result.ok(category1List);
    }

    /**
     * 查询二级分类信息
     * @param category1Id 一级分类ID
     * @return
     */
    @GetMapping("getCategory2/{category1Id}")
    public Result<List<BaseCategory2>> getCategory2(@PathVariable Long category1Id){
        List<BaseCategory2> category2List = manageService.getCategory2(category1Id);
        return Result.ok(category2List);
    }

    /**
     * 查询三级分类信息
     * @param category2Id 二级分类信息
     * @return
     */
    @GetMapping("getCategory3/{category2Id}")
    public Result<List<BaseCategory3>> getCategory3(@PathVariable Long category2Id){
        List<BaseCategory3> category3List = manageService.getCategory3(category2Id);
        return Result.ok(category3List);
    }

    /**
     * 根据页面输入的分类ID  查询该分类下的属性数据
     * @param category1Id 一级分类ID
     * @param category2Id 二级分类ID
     * @param category3Id 三级分类ID
     * @return
     */
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result<List<BaseAttrInfo>> getCategory3(@PathVariable Long category1Id,
                                                    @PathVariable Long category2Id,
                                                    @PathVariable Long category3Id){
        List<BaseAttrInfo> attrInfoList = manageService.getAttrInfoList(category1Id, category2Id, category3Id);
        return Result.ok(attrInfoList);
    }

    /**
     * 保存或修改平台属性
     * @param baseAttrInfo 前台传递的值  @RequestBody：将JSON字符串转换为Java对象
     * @return
     */
    @PostMapping("saveAttrInfo")
    public Result<Integer> saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        manageService.saveInfo(baseAttrInfo);
        return Result.ok();
    }

    /**
     * 修改平台属性：根据属性ID查询属性信息
     * @param attrId
     * @return
     */
    @GetMapping("getAttrValueList/{attrId}")
    public Result<List<BaseAttrValue>> getAttrValueList(@PathVariable("attrId") Long attrId) {
        //根据属性ID获取属性信息和属性值信息
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);
        //获取该属性ID对应的属性值信息集合
        List<BaseAttrValue> baseAttrValueList = baseAttrInfo.getAttrValueList();
        return Result.ok(baseAttrValueList);
    }

    /**
     * 根据条件查询spuinfo
     * @param page
     * @param limit
     * @param spuInfo
     * @return
     */
    @GetMapping("{page}/{limit}")
    public Result getPageList(@PathVariable Long page,@PathVariable Long limit,SpuInfo spuInfo){
        //创建一个page对象
        Page<SpuInfo> spuInfoPage = new Page<>();
        //调用服务层 查询spuinfo数据信息
        IPage<SpuInfo> spuInfoIPage = manageService.selectPage(spuInfoPage, spuInfo);
        return Result.ok(spuInfoIPage);
    }


}
