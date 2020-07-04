package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author smy
 * @BelongsProject: gmallparent
 * @BelongsPackage: com.atguigu.gmall.all.controller
 * @create 2020-06-17 16:16
 * @Description:
 */
@Controller
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    //当用户访问 / 或者 index.html 时都可以显示首页信息
    @GetMapping({"/","index.html"})
    public String index(HttpServletRequest request){

        //获取首页分类数据
        Result result = productFeignClient.getBaseCategoryList();
        request.setAttribute("list",result.getData());

        return "index/index";
    }

}
