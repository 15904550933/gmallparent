package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeginClient;
import com.atguigu.gmall.model.list.SearchParam;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author smy
 * @BelongsProject: gmallparent
 * @BelongsPackage: com.atguigu.gmall.all.controller
 * @create 2020-06-22 9:34
 * @Description:
 */
@Controller
public class ListController {

    @Autowired
    private ListFeginClient listFeignClient;

    @GetMapping("list.html")
    public String search(SearchParam searchParam, Model model) {

        Result<Map> list = listFeignClient.list(searchParam);
        model.addAllAttributes(list.getData());

        // 记录拼接url；
        String urlParam = makeUrlParam(searchParam);

        //获取处理品牌的数据
        String tradeMark = makeTradeMark(searchParam.getTrademark());

        //获取平台属性的数据
        List<Map<String, String>> makePropsList = makeProps(searchParam.getProps());

        //获取排序规则
        Map<String, Object> order = order(searchParam.getOrder());

        //存储数据
        model.addAttribute("urlParam", urlParam);
        model.addAttribute("searchParam", searchParam);
        model.addAttribute("tradeMarkParam", tradeMark);
        model.addAttribute("propsParamList", makePropsList);
        //orderMap中应该有type,sort字段
        model.addAttribute("orderMap", order);

        System.out.println("***********searchParam = " + searchParam);

        //检索列表
        return "list/index";
    }

    /**
     * 处理排序,根据页面提供的数据
     */
    private Map<String,Object> order(String order){
        HashMap<String, Object> hashMap = new HashMap<>();
        if (!StringUtils.isEmpty(order)){
            //数据进行分割
            String[] split = order.split(":");
            if (null != split && split.length == 2){
                //数据处理
                //type只按照什么规则排序 综合 | 价格
                hashMap.put("type", split[0]);
                //排序规则 desc | asc
                hashMap.put("sort", split[1]);
            }else {
                //给一个默认排序规则
                hashMap.put("type", "1");
                //排序规则 desc | asc
                hashMap.put("sort", "asc");
            }
        }else {
            //给一个默认排序规则
            hashMap.put("type", "1");
            //排序规则 desc | asc
            hashMap.put("sort", "asc");
        }
        return hashMap;
    }


    /**
     * 拼接检索条件
     *
     * @param searchParam
     * @return
     */
    private String makeUrlParam(SearchParam searchParam) {
        StringBuilder urlParam = new StringBuilder();
        //用户截所入口只有两个，一个是三级分类Id,一个是全文检索keyWord

        if(null!=searchParam.getKeyword()){
            //说明用户通过关键字入口进行检索
            //http://list.gmall.com/list.html?keyword=小米手机
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }
        if (null != searchParam.getCategory3Id()) {
            //说明用户通过三级分类Id进行检索
            //http://list.gmall.com/list.html?category3Id=61
            urlParam.append("category3Id=").append(searchParam.getCategory3Id());
        }
        if (null != searchParam.getCategory2Id()) {
            //说明用户通过二级分类Id进行检索
            //http://list.gmall.com/list.html?category2Id=13
            urlParam.append("category2Id=").append(searchParam.getCategory2Id());
        }
        if (null != searchParam.getCategory1Id()) {
            //说明用户通过一级分类Id进行检索
            //http://list.gmall.com/list.html?category1Id=2
            urlParam.append("category1Id=").append(searchParam.getCategory1Id());
        }

        //通过两个入口 进来之后 ，还可以通过品牌检索
        if (null != searchParam.getTrademark()) {
            //http://list.gmall.com/list.html?category3Id=61&trademark=2:华为
            //http://list.gmall.com/list.html?keyword=小米手机&trademark=2:华为
            if (urlParam.length() > 0) {
                urlParam.append("&trademark=").append(searchParam.getTrademark());
            }
        }

        //还可以选择平台属性值
        if (null != searchParam.getProps()) {
            //http://list.gmall.com/list.html?category3Id=61&props=1:2800-4499:价格&props=2:6.56-6.7英寸:屏幕尺寸
            //http://list.gmall.com/list.html?keyword=小米手机&props=1:2800-4499:价格&props=2:6.56-6.7英寸:屏幕尺寸
            for (String prop : searchParam.getProps()) {
                if (urlParam.length() > 0) {
                    urlParam.append("&props=").append(prop);
                }
            }

        }

        return "list.html?" + urlParam.toString();
    }

    //处理品牌 ---> 品牌：品牌的名称
    //注意：传入的参数应该与封装的实体类中得品牌属性一致
    private String makeTradeMark(String tradeMark) {
        //用户点击的哪个品牌
        if (!StringUtils.isEmpty(tradeMark)) {
            //tradeMark=2:华为
            String[] split = tradeMark.split(":");
            if (null != split && split.length == 2) {
                return "品牌：" + split[1];
            }
        }
        return null;
    }

    //处理平台属性 ---> 平台属性名称：平台属性值名称
    //分析数据存储的格式List<Map<String,String>>
    private List<Map<String, String>> makeProps(String[] props) {
        //声明一个集合
        List<Map<String, String>> list = new ArrayList<>();
        //数据格式:props=23:4G:运行内存
        if (null != props && props.length > 0) {
            //开始循环
            for (String prop : props) {
                String[] split = prop.split(":");
                //保证数据格式
                if (null != split && split.length == 3) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("attrId", split[0]);
                    map.put("attrValue", split[1]);
                    map.put("attrName", split[2]);
                    //添加到list集合中
                    list.add(map);
                }
            }
        }
        return list;
    }



}
