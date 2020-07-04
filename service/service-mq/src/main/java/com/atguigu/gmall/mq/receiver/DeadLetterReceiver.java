package com.atguigu.gmall.mq.receiver;

import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@Configuration
public class DeadLetterReceiver {

    @RabbitListener(queues = DeadLetterMqConfig.QUEUE_DEAD_2)
    public void get(String msg) {
        System.out.println("接收数据:" + msg);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("接收 队列2: " + sdf.format(new Date()) + " Delay rece." + msg);
    }



}
