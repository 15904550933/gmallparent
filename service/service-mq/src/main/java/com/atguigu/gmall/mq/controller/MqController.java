package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/mq")
@Slf4j
public class MqController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitService rabbitService;



    /**
     * 消息发送的方法
     *
     * @return
     */
    //http://cart.gmall.com/8282/mq/sendConfirm
    @GetMapping("sendConfirm")
    public Result sendConfirm() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //来自rebbit-util 中 RabbitService的对象
        rabbitService.sendMessage("exchange.confirm", "routing.confirm", sdf.format(new Date()));
        return Result.ok();
    }

    /**
     * 测试死信队列，向队列一发送数据
     * 方式一
     *      http://cart.gmall.com:8282/mq/sendDeadLettle
     * @return
     */
//    @GetMapping("sendDeadLettle")
//    public Result sendDeadLettle() {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        /*准备发送消息：*/
//        //方式一：指定每个消息延迟时间，存在问题：后进入队列会阻塞先进入队列超时消息，原因：先进先出，即使超时也出不来
//        this.rabbitTemplate.convertAndSend(DeadLetterMqConfig.EXCHANGE_DEAD, DeadLetterMqConfig.ROUTING_DEAD_1,
//                "OK", message -> {
//            //设置消息发送的延迟时间
//            // 如果配置了 params.put("x-message-ttl", 5 * 1000); 那么这一句也可以省略,具体根据业务需要是声明 Queue 的时候就指定好延迟时间还是在发送自己控制时间
//            message.getMessageProperties().setExpiration(1 * 1000 * 10 + "");
//            System.out.println(sdf.format(new Date()) + " Delay sent.");
//            return message;
//        });
//
//        return Result.ok();
//    }

    /**
     * 方式二
     * @return
     */
    @GetMapping("sendDeadLettle")
    public Result sendDeadLettle() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.rabbitTemplate.convertAndSend(DeadLetterMqConfig.EXCHANGE_DEAD, DeadLetterMqConfig.ROUTING_DEAD_1, "OKK");
        System.out.println(sdf.format(new Date()) + " Delay sent.");
        return Result.ok();
    }


    @GetMapping("sendDelay")
    public Result sendDelay() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.rabbitTemplate.convertAndSend(DelayedMqConfig.EXCHANGE_DELAY, DelayedMqConfig.ROUTING_DELAY, sdf.format(new Date()), new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().setDelay(10 * 1000);
                System.out.println(sdf.format(new Date()) + " Delay sent..");
                return message;
            }
        });
        return Result.ok();
    }




}
