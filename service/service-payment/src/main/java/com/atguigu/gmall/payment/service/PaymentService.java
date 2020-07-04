package com.atguigu.gmall.payment.service;

import com.alipay.api.AlipayApiException;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    /**
     * 保存交易记录
     *
     * @param orderInfo
     * @param paymentType 支付类型（1：微信 2：支付宝）
     */
    void savePaymentInfo(OrderInfo orderInfo, String paymentType);

    /**
     * 获取交易记录信息
     *
     * @param out_trade_no
     * @param name
     * @return
     */
    PaymentInfo getPaymentInfo(String out_trade_no, String name);

    /**
     * 支付成功
     *
     * @param outTradeNo
     * @param paymentType
     * @param paramMap
     */
    void paySuccess(String outTradeNo, String paymentType, Map<String, String> paramMap);

    /**
     * 根据第三方交易编号更新交易记录
     * @param outTradeNo
     * @param paymentInfo
     */
    void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo);

    /**
     * 关闭过期交易记录
     * @param orderId
     */
    void closePayment(Long orderId);
}
