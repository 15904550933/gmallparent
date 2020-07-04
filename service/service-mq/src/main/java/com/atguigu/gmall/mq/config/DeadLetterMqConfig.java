package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DeadLetterMqConfig {

    public static final String EXCHANGE_DEAD = "exchange.dead";
    public static final String ROUTING_DEAD_1 = "routing.dead.1";
    public static final String ROUTING_DEAD_2 = "routing.dead.2";
    public static final String QUEUE_DEAD_1 = "queue.dead.1";
    public static final String QUEUE_DEAD_2 = "queue.dead.2";

    /**
     * 其他队列可以在RabbitListener上面做绑定
     * 定义交换机
     *
     * @return
     */
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_DEAD, true, false, null);
    }

    /**
     * 定义队列一
     * 带参数
     *
     * @return
     */
    @Bean
    public Queue queue1(){
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", EXCHANGE_DEAD);
        arguments.put("x-dead-letter-routing-key", ROUTING_DEAD_2);
        //方式二，统一延迟时间
        arguments.put("x-message-ttl", 10 * 1000);

        //方式一与方式二切换时，必须先删除对应交换机与队列，否则出错
        return new Queue(QUEUE_DEAD_1, true, false, false, arguments);
    }


    /**
     * 队列一的绑定规则
     *
     * @return
     */
    @Bean
    public Binding binding() {
        //将队列一通过ROUTING_DEAD_1  key  绑定到EXCHANGE_DEAD交换机上
        return BindingBuilder.bind(queue1()).to(exchange()).with(ROUTING_DEAD_1);
    }

    /**
     * 队列二
     * 普通的队列
     *
     * @return
     */
    @Bean
    public Queue queue2() {
        return new Queue(QUEUE_DEAD_2, true, false, false, null);
    }

    /**
     * 队列二的绑定规则
     *
     * @return
     */
    @Bean
    public Binding deadBinding() {
        //将队列二通过 ROUTING_DEAD_2 key 绑定到  EXCHANGE_DEAD 交换机
        return BindingBuilder.bind(queue2()).to(exchange()).with(ROUTING_DEAD_2);
    }
}
