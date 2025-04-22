package com.graduation.his.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.graduation.his.utils.security.StpPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * SaToken WebSocket通道拦截器
 * 用于WebSocket连接时的用户认证
 * @author hua
 */
@Slf4j
@Component
public class SaTokenChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("WebSocket连接请求，开始处理认证...");
            
            // 获取token，可能在原生头信息或STOMP头信息中
            String token = accessor.getFirstNativeHeader("sa-token-authorization");
            
            // 如果原生头信息中没有，则尝试从STOMP头信息中获取
            if (!StringUtils.hasLength(token)) {
                token = accessor.getFirstNativeHeader("token");
            }
            
            if (StringUtils.hasLength(token)) {
                try {
                    // 用SaToken校验token并获取loginId
                    Object loginId = StpUtil.getLoginIdByToken(token);
                    if (loginId != null) {
                        log.info("WebSocket认证成功，用户ID: {}", loginId);
                        
                        // 将用户身份信息绑定到会话
                        accessor.setUser(new StpPrincipal(loginId.toString()));
                    } else {
                        log.warn("WebSocket认证失败: token无效");
                    }
                } catch (Exception e) {
                    log.error("WebSocket认证失败: {}", e.getMessage());
                    // 认证失败不需要抛出异常，会在后续逻辑中被处理
                }
            } else {
                log.warn("WebSocket连接未提供认证token");
            }
        }
        
        return message;
    }
} 