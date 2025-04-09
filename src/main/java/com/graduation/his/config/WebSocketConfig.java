package com.graduation.his.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置类
 * @author hua
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 设置消息代理前缀，客户端订阅时使用
        registry.enableSimpleBroker("/topic", "/queue");
        // 设置应用前缀，客户端发送消息时使用
        registry.setApplicationDestinationPrefixes("/app");
        // 设置用户前缀，点对点推送时使用
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点，客户端连接时使用
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // 允许跨域
                .withSockJS(); // 启用SockJS
    }
} 