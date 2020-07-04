package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.client.naming.utils.StringUtils;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentFeignClient paymentFeignClient;

    @Autowired
    private RabbitService rabbitService;

    /**
     * 取消订单消费者
     * 延迟队列，不能再这里做交换机与队列绑定
     *
     * @param orderId
     * @throws IOException
     */
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel) throws IOException {
        //判断订单Id是否为空
        if (null != orderId) {
            //防止重复消费，判断订单状态
            OrderInfo orderInfo = orderService.getById(orderId);
            //订单状态是未支付 涉及关闭orderInfo,paymenyInfo,alipay
            if (null != orderInfo && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())) {
//                //关闭过期订单
//                orderService.execExpiredOrder(orderId);

                //订单创建时候就是未付款，判断是否有交易记录产生
                PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                if (null != paymentInfo && paymentInfo.getPaymentStatus().equals(PaymentStatus.UNPAID.name())){
                    //先查看是否有交易记录（扫描二维码才有交易记录）
                    Boolean aBoolean = paymentFeignClient.checkPayment(orderId);
                    if (aBoolean){
                        //有交易记录 关闭支付宝交易记录
                        Boolean flag = paymentFeignClient.closePay(orderId);
                        //防止用户在过期时间快到的时候付款
                        if (flag){
                            //用户没有付款，关闭订单，关闭交易记录 2:表示要关闭交易记录paymentInfo中有记录
                            orderService.execExpiredOrder(orderId, "2");
                        }else{
                            //用户已经付款
                            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY, MqConst.ROUTING_PAYMENT_PAY, orderId);
                        }
                    }else{
                        //在支付宝中没有交易记录，但是在电商中有交易记录
                        orderService.execExpiredOrder(orderId, "2");
                    }
                }else{
                    //paymentInfo中没有建议记录
                    orderService.execExpiredOrder(orderId, "1");
                }
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }


    /**
     * 订单支付，更改订单状态与通知扣减库存
     *
     * @param orderId
     * @throws IOException
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void paySuccess(Long orderId, Message message, Channel channel) throws IOException {
        if (null != orderId) {
            //防止重复消费
            OrderInfo orderInfo = orderService.getById(orderId);
            //判断状态
            if (null != orderInfo && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())) {
                // 支付成功！ 修改订单状态为已支付
                System.out.println("orderId = " + orderId + "--> 修改订单状态为已支付");
                //更新数据
                orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
                // 发送消息，通知仓库
                orderService.sendOrderStatus(orderId);
            }
        }
        //手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    /**
     * 扣减库存成功，更新订单状态
     *
     * @param msgJson
     * @throws IOException
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void updateOrderStatus(String msgJson, Message message, Channel channel) throws IOException {
        //获取Json数据
        if (!StringUtils.isEmpty(msgJson)) {
            Map<String, Object> map = JSON.parseObject(msgJson, Map.class);
            String orderId = (String) map.get("orderId");
            String status = (String) map.get("status");

            //根据status判断减库存结果
            if ("DEDUCTED".equals(status)) {
                // 减库存成功！ 修改订单状态为已支付
                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.WAITING_DELEVER);
            } else {
                /*
                    减库存失败！远程调用其他仓库查看是否有库存！
                    true:   orderService.sendOrderStatus(orderId); orderService.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
                    false:  1.  补货  | 2.   人工客服。
                 */
                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.STOCK_EXCEPTION);
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

}
