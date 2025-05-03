package com.graduation.his.utils.rabbitmq;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

public class QueueCreateTest {

    @Test
    public void testCreateUserQueue() {
        // 1. 创建连接工厂 (替换为你自己的RabbitMQ地址、用户、密码、vhost)
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("1.95.79.29", 5672);
        connectionFactory.setUsername("hros");
        connectionFactory.setPassword("123321");
        connectionFactory.setVirtualHost("/hros");

        // 2. 创建RabbitAdmin
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);

        // 3. 构建队列参数
        String queueName = "user.queue.17";
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "dead.letter.exchange");
        args.put("x-dead-letter-routing-key", "dead.letter.routing.key");
        args.put("x-message-ttl", 2592000000L); // 30天（毫秒）
        args.put("x-queue-mode", "lazy");

        // 4. 声明队列
        try {
            Queue queue = new Queue(queueName, true, false, false, args);
            rabbitAdmin.declareQueue(queue);
            System.out.println("队列创建成功: " + queueName);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connectionFactory.destroy();
        }
    }
}