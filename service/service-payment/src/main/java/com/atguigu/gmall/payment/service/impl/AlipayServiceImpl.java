package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;


@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private AlipayClient alipayClient;

    @Override
    public String createaliPay(Long orderId) throws AlipayApiException {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        // 保存交易记录
        paymentService.savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());
        // 生产二维码
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        // 同步回调
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        // 异步回调
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址
        // 参数

        // 声明一个map 集合
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no", orderInfo.getOutTradeNo());
        map.put("product_code", "FAST_INSTANT_TRADE_PAY");
        map.put("total_amount", orderInfo.getTotalAmount());
        map.put("subject", orderInfo.getTradeBody());

        alipayRequest.setBizContent(JSON.toJSONString(map));
        //直接将完整的表单返回
        return alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单;
    }

    /**
     * 退款
     *
     * @param orderId
     * @return
     */
    @Override
    public boolean refund(Long orderId) {

        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

        //获取订单对象
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);

        //声明一个map集合来存储数据
        HashMap<String, Object> map = new HashMap<>();

        //out_trade_no与trade_no二选一   out_trade_no --> 支付宝交易编号 order_info中与payment_info中相同
        map.put("out_trade_no", orderInfo.getOutTradeNo());
        //退多少钱
        map.put("refund_amount", orderInfo.getTotalAmount());
        //退款原因
        map.put("refund_reason", "退款原因？");
        // out_request_no --> 如果部分退款，传此参数
//        map.put("out_request_no", "HZ01RF001");

        request.setBizContent(JSON.toJSONString(map));


        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            // 更新交易记录 ： 关闭
            System.out.println("调用成功");
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
            //根据第三方交易编号更新交易记录
            paymentService.updatePaymentInfo(orderInfo.getOutTradeNo(), paymentInfo);
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }


    /**
     * 支付宝关闭交易接口
     * @param orderId
     * @return
     */
    @SneakyThrows
    @Override
    public Boolean closePay(Long orderId) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        // PaymentInfo paymentInfo = paymentService.getPaymentInfo(orderInfo.getOutTradeNo(), PaymentType.ALIPAY.name());
        // 引入aliPayClient
        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();

        //创建一个Map
        HashMap<String, Object> map = new HashMap<>();
        // map.put("trade_no",paymentInfo.getTradeNo()); // 从paymentInfo 中获取！
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("operator_id","YX01");
        request.setBizContent(JSON.toJSONString(map));

        AlipayTradeCloseResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    /**
     * 根据订单查询是否支付成功！
     *
     * @param orderId
     * @return
     */
    @SneakyThrows
    @Override
    public Boolean checkPayment(Long orderId) {
        // 根据订单Id 查询订单信息
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        // 根据out_trade_no 查询交易记录
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            return true;
        } else {
            return false;
        }
    }



}
