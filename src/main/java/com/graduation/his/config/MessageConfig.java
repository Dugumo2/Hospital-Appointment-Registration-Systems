package com.graduation.his.config;

import com.graduation.his.common.Constants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author hua
 * @description 消息配置类（RabbitMQ基础配置）
 * @create 2025-04-09 13:02
 */
@Configuration
public class MessageConfig {

    @Bean
    public MessageConverter messageConverter() {
        // 1.定义消息转换器
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
        return jackson2JsonMessageConverter;
    }

    /**
     * 配置RabbitTemplate，使用JSON消息转换器
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
    
    /**
     * 配置反馈消息交换机(Topic类型)
     */
    @Bean
    public TopicExchange feedbackExchange() {
        return new TopicExchange(Constants.MessageKey.FEEDBACK_MESSAGE_QUEUE, true, false);
    }
    
    /**
     * 配置死信交换机，用于处理过期的消息
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("dead.letter.exchange", true, false);
    }
    
    /**
     * 配置死信队列，用于接收过期的消息
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("dead.letter.queue")
                .withArgument("x-queue-mode", "lazy") // 将队列设置为lazy模式
                .build();
    }
    
    /**
     * 将死信队列绑定到死信交换机
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("dead.letter.routing.key");
    }

    @ConditionalOnProperty(name = "spring.rabbitmq.listener.simple.retry.enabled", havingValue = "true")
    @Bean
    public DirectExchange errorMessageExchange() {
        return new DirectExchange("error.direct");
    }

    @ConditionalOnProperty(name = "spring.rabbitmq.listener.simple.retry.enabled", havingValue = "true")
    @Bean
    public Queue errorQueue() {
        return new Queue("error.queue", true);
    }

    @ConditionalOnProperty(name = "spring.rabbitmq.listener.simple.retry.enabled", havingValue = "true")
    @Bean
    public Binding errorBinding(Queue errorQueue, DirectExchange errorMessageExchange) {
        return BindingBuilder.bind(errorQueue).to(errorMessageExchange).with("error");
    }

    @ConditionalOnProperty(name = "spring.rabbitmq.listener.simple.retry.enabled", havingValue = "true")
    @Bean
    public MessageRecoverer republishMessageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, "error.direct", "error");
    }

    /**
     * 创建用户反馈消息队列
     */
    @Bean
    public Queue feedbackQueue() {
        return QueueBuilder.durable("feedback.message.queue")
                .withArgument("x-dead-letter-exchange", "dead.letter.exchange")
                .withArgument("x-dead-letter-routing-key", "dead.letter.routing.key")
                .withArgument("x-message-ttl", 7 * 24 * 60 * 60 * 1000) // 消息7天过期
                .withArgument("x-queue-mode", "lazy") // 将队列设置为lazy模式
                .build();
    }

    /**
     * 将反馈消息队列绑定到交换机
     */
    @Bean
    public Binding feedbackBinding(Queue feedbackQueue, TopicExchange feedbackExchange) {
        return BindingBuilder.bind(feedbackQueue)
                .to(feedbackExchange)
                .with("user.*"); // 使用通配符匹配所有用户
    }
}
