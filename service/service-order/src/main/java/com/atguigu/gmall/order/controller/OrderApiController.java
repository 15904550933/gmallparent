package com.atguigu.gmall.order.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.spring.web.json.Json;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author smy
 * @date 2020/6/24 15:21
 */
@RestController
@RequestMapping("api/order")
public class OrderApiController {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;


    @Autowired
    private OrderService orderService;



    // 订单 在网关中设置过这个拦截 /api/**/auth/** 必须登录才能访问
    @GetMapping("auth/trade")
    public Result<Map<String, Object>> trade(HttpServletRequest request) {
        // 登录之后的用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 获取用户地址列表 根据用户Id
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);

        // 获取送货清单
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);

        // 声明一个OrderDetail 集合
        List<OrderDetail> orderDetailList = new ArrayList<>();

        int totalNum = 0;
        // 循环遍历，将数据赋值给orderDetail
        if (!CollectionUtils.isEmpty(cartCheckedList)) {
            // 循环遍历
            for (CartInfo cartInfo : cartCheckedList) {
                // 将cartInfo 赋值给 orderDetail
                OrderDetail orderDetail = new OrderDetail();

                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setOrderPrice(cartInfo.getSkuPrice());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setSkuId(cartInfo.getSkuId());
                // 计算每个商品的总个数。
                totalNum += cartInfo.getSkuNum();
                // 将每一个orderDeatil 添加到集合中
                orderDetailList.add(orderDetail);
            }
        }


        // 声明一个map 集合来存储数据
        Map<String, Object> map = new HashMap<>();
        // 存储订单明细
        map.put("detailArrayList", orderDetailList);
        // 存储收货地址列表
        map.put("userAddressList", userAddressList);
        // 存储总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        // 计算总金额
        orderInfo.sumTotalAmount();
        map.put("totalAmount", orderInfo.getTotalAmount());
        // 存储商品的件数 记录大的商品有多少个
        map.put("totalNum", orderDetailList.size());

        // 获取流水号
        String tradeNo = orderService.getTradeNo(userId);
        map.put("tradeNo", tradeNo);

        // 计算小件数：
        // map.put("totalNum",totalNum);

        return Result.ok(map);
    }

    /**
     * 提交订单
     *      防止回退无刷新提交
     *      验证库存
     *      验证价格
     * @param orderInfo
     * @param request
     * @return
     */
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        // 获取到用户Id
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));


        /*
        获取前台页面的流水号,防止回退无刷新提交
         */
        String tradeNo = request.getParameter("tradeNo");
        // 调用服务层的比较方法
        boolean flag = orderService.checkTradeCode(userId, tradeNo);
        if (!flag) {
            // 比较失败！说明回退无刷新提交
            return Result.fail().message("不能回退无刷新重复提交订单！");
        }
        //  删除流水号
        orderService.deleteTradeNo(userId);


        /**
        验证库存&价格：
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            // 验证库存：
            boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!result) {
                return Result.fail().message(orderDetail.getSkuName() + "库存不足！");
            }
            // 验证价格：
            BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
            if (orderDetail.getOrderPrice().compareTo(skuPrice) != 0) {
                // 重新查询价格！
                // 订单的价格来自于购物车，更改购物车中价格
                cartFeignClient.loadCartCache(userId);
                return Result.fail().message(orderDetail.getSkuName() + "价格有变动！");
            }
        }
         */
        /**
         * 使用异步编排
         */
        //创建一个对象，存储错误信息
        List<String> errorList = new ArrayList<>();
        //声明一个集合在存储异步编排对象
        List< CompletableFuture> futureList = new ArrayList<>();
        // 验证库存：
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            //开一个异步编排
            CompletableFuture<Void> checkStockCompletableFuture = CompletableFuture.runAsync(() -> {
                // 验证库存：
                boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                if (!result) {
                    //提示信息，库存不足
                    errorList.add(orderDetail.getSkuName() + " --> 库存不足！");
                }
            }, threadPoolExecutor);
            futureList.add(checkStockCompletableFuture);

            CompletableFuture<Void> checkPriceCompletableFuture = CompletableFuture.runAsync(() -> {
                // 验证价格：
                BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                if (orderDetail.getOrderPrice().compareTo(skuPrice) != 0) {
                    // 重新查询价格！
                    cartFeignClient.loadCartCache(userId);
                    errorList.add(orderDetail.getSkuName() + "价格有变动！");
                }
            }, threadPoolExecutor);
            futureList.add(checkPriceCompletableFuture);
        }

        //合并线程 所有的异步编排都在futureList
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()])).join();
        if(errorList.size() > 0) {
            //获取异常集合中得数据
            return Result.fail().message(StringUtils.join(errorList, ","));
        }


        // 验证通过，保存订单！
        Long orderId = orderService.saveOrderInfo(orderInfo);


        return Result.ok(orderId);
    }

    /**
     * 根据订单Id,查询订单数据对象。内部调用获取订单
     * @param orderId
     * @return
     */
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable(value = "orderId") Long orderId){
        return orderService.getOrderInfo(orderId);
    }

    /**
     * 拆单接口
     * @return
     */
    @RequestMapping("orderSplit")
    public String orderSplit(HttpServletRequest request){

        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");

        //参考拆单接口,获取子订单集合
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(Long.parseLong(orderId), wareSkuMap);

        //声明一个存储map的集合
        List<Map> mapList = new ArrayList<>();

        //将子订单的部分数据转换为json字符串
        for (OrderInfo orderInfo : subOrderInfoList) {
            //将部分数据转换为map
            Map map = orderService.initWareOrder(orderInfo);
            mapList.add(map);
        }
        //返回子订单的JSON字符串
        return JSON.toJSONString(mapList);



    }

    /**
     * 秒杀提交订单，秒杀订单不需要做前置判断，直接下单
     * @param orderInfo
     * @return
     */
    @PostMapping("inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo) {
        //调用保存订单的方法
        Long orderId = orderService.saveOrderInfo(orderInfo);
        //返回订单Id
        return orderId;
    }

}
