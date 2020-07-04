package com.atguigu.gmall.mq.receiver;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Configuration
public class ConfirmReceiver {

    /**
     * 消息接收者
     * @param message
     * @param channel
     */
    //@SneakyThrows ---> lombok
//    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            //value = @Queue（value = "队列名", autoDelete = "自动删除"）,
            value = @Queue(value = "queue.confirm", autoDelete = "false"),
            //exchange = @Exchange(value = "交换机", autoDelete = "自动删除"),
            exchange = @Exchange(value = "exchange.confirm", autoDelete = "true"),
            //key = {"路由键"})
            key = {"routing.confirm"}))
    public void process(Message message, Channel channel) {
        //获取消息
        System.out.println("msg : " + new String(message.getBody()));

        // 采用手动应答模式, 手动确认应答更为安全稳定
        //如果手动确定了，再出异常，mq不会通知；如果没有手动确认，抛异常mq会一直通知
        //channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        try {
            int i = 1 / 0;
            // 确认消息  false 确认一个消息，true 批量确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // 消息是否再次被拒绝！
            System.out.println("发生异常!" + e.getMessage());
            // getRedelivered() 判断是否已经处理过一次消息！
            if (message.getMessageProperties().getRedelivered()) {
                System.out.println("消息已重复处理,拒绝再次接收");
                // 拒绝消息，requeue=false 表示不再重新入队，如果配置了死信队列则进入死信队列
                try {
                    channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            } else {
                System.out.println("消息即将再次返回队列处理");
                // 参数二：是否批量， 参数三：为是否重新回到队列，true重新入队
                try {
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }
}
