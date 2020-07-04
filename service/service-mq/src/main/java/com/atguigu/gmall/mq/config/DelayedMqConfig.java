package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DelayedMqConfig {

    public static final String EXCHANGE_DELAY = "exchange.delay";
    public static final String ROUTING_DELAY = "routing.delay";
    public static final String QUEUE_DELAY_1 = "queue.delay.1";

    /**
     * 队列不要在RabbitListener上面做绑定，否则不会成功，如队列2，必须在此绑定
     *
     * @return
     */

    @Bean
    public Queue delayQeue1() {
        // 返回队列
        // 第一个参数是创建的queue的名字，第二个参数是是否支持持久化
        return new Queue(QUEUE_DELAY_1, true);
    }

    @Bean
    public CustomExchange delayExchange() {
        //配置参数
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(EXCHANGE_DELAY, "x-delayed-message", true, false, args);
    }

    /**
     * 设置队列二得绑定关系
     *
     * @return
     */
    @Bean
    public Binding delayBbinding1() {
        return BindingBuilder.bind(delayQeue1()).to(delayExchange()).with(ROUTING_DELAY).noargs();
    }

}
