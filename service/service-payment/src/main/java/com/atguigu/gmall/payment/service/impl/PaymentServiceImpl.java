package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.naming.Name;
import java.util.Date;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {
        //交易记录中如果有当前对应的订单Id时，不能插入当前数据
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderInfo.getId());
        queryWrapper.eq("payment_type", paymentType);
        Integer count = paymentInfoMapper.selectCount(queryWrapper);
        if (count > 0) {
            return;
        }
        // 保存交易记录
        PaymentInfo paymentInfo = new PaymentInfo();
        // 给对象赋值
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        //paymentInfo.setSubject("test");
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());

        paymentInfoMapper.insert(paymentInfo);
    }

    /**
     * 根据out_trade_no以及支付方式查询交易记录
     *
     * @param out_trade_no
     * @param name
     * @return
     */
    @Override
    public PaymentInfo getPaymentInfo(String out_trade_no, String name) {
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no", out_trade_no).eq("payment_type", name);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
        return paymentInfo;

    }

    @Override
    public void paySuccess(String outTradeNo, String paymentType, Map<String, String> paramMap) {
        //需要的获取到订单Id
        PaymentInfo paymentInfo = this.getPaymentInfo(outTradeNo, paymentType);
        //如果当前订单交易记录中已经是付款完成的或者交易关闭的，则后续业务不会执行
        if (paymentInfo.getPaymentStatus() == PaymentStatus.PAID.name()
                || paymentInfo.getPaymentStatus() == PaymentStatus.ClOSED.name()) {
            return;
        }


        //第一个参数：更新得内容， 第二个参数：更新的条件
        PaymentInfo paymentInfoUpd = new PaymentInfo();
        // update paymentInfo set PaymentStatus = PaymentStatus.PAID ,CallbackTime = new Date() where out_trade_no = ?
        paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfoUpd.setCallbackTime(new Date());
        //更新支付宝的交易号，交易号在map中
        paymentInfoUpd.setTradeNo(paramMap.get("trade_no"));
        paymentInfoUpd.setCallbackContent(paramMap.toString());

        //构造更新条件：根据第三方交易编号，修改支付交易记录
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("out_trade_no", outTradeNo)
                .eq("payment_type", paymentType);
        paymentInfoMapper.update(paymentInfoUpd, queryWrapper);


        // 后续更新订单状态！ 使用消息队列！
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY, MqConst.ROUTING_PAYMENT_PAY, paymentInfo.getOrderId());

    }

    /**
     * 根据第三方交易编号更新交易记录
     *
     * @param outTradeNo
     * @param paymentInfo
     */
    @Override
    public void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo) {
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("out_trade_no", outTradeNo);
        paymentInfoMapper.update(paymentInfo, queryWrapper);
    }

    /**
     * 关闭过期交易记录
     * 关闭 orderInfo paymentInfo Alipay
     * @param orderId
     */
    @Override
    public void closePayment(Long orderId) {
        // 设置关闭交易记录的条件  118
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id", orderId);
        // 如果当前的交易记录不存在，则不更新交易记录
        Integer count = paymentInfoMapper.selectCount(paymentInfoQueryWrapper);
        if (null == count || count.intValue() == 0) {
            //说明这个订单没有交易记录
            return;
        }
        // 关闭订单 update payment_info set PaymentStatus = CLOSED where order_id = orderId
        // 在关闭支付宝交易之前。还需要关闭paymentInfo
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
        paymentInfoMapper.update(paymentInfo, paymentInfoQueryWrapper);

    }

}
