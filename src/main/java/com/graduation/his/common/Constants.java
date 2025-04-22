package com.graduation.his.common;

import java.security.PublicKey;

/**
 * 系统常量类
 */
public class Constants {
    
    /**
     * 密码加密盐
     */
    public static final String SALT = "HIS_SALT_2025#@!";
    
    /**
     * MinIO存储相关常量
     */
    public static class MinioConstants {
        /**
         * 用户头像存储桶名称
         */
        public static final String USER_AVATAR_BUCKET = "his-user-avatars";
        
        /**
         * 默认用户头像URL
         */
        public static final String DEFAULT_AVATAR_URL = "https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png";
    }
    
    /**
     * Redis Key前缀
     */
    public static class RedisKey {
        /**
         * 邮箱验证码前缀
         */
        public static final String HIS_MAIL_CODE = "his:mail:code:";
        
        /**
         * AI问诊会话前缀
         */
        public static final String AI_CONSULT_SESSION = "ai_consult:session:";
        
        /**
         * AI问诊锁前缀
         */
        public static final String AI_CONSULT_LOCK = "ai_consult:lock:";

        /**
         * 用户未读消息前缀
         */
        public static final String MESSAGE_USER = "message_user:";
    }

    public static class MessageKey{
        /**
         * 反馈消息交换机
         */
        public static final String FEEDBACK_MESSAGE_QUEUE = "hros.topic";
        
        /**
         * 反馈消息队列名
         */
        public static final String FEEDBACK_QUEUE_NAME = "feedback.message.queue";
        
        /**
         * 用户消息路由键前缀
         */
        public static final String USER_ROUTING_KEY_PREFIX = "user.";
    }
    
    /**
     * AI服务相关常量
     */
    public static class AIConstants {
        /**
         * 会话过期时间（6小时）
         */
        public static final long SESSION_EXPIRE_HOURS = 1;
        
        /**
         * 分布式锁等待时间（秒）
         */
        public static final long LOCK_WAIT_TIME = 5;
        
        /**
         * 分布式锁租约时间（秒）
         */
        public static final long LOCK_LEASE_TIME = 10;
    }
    
    /**
     * WebSocket相关常量
     */
    public static class WebSocketConstants {
        /**
         * 诊后反馈消息队列路径
         */
        public static final String FEEDBACK_QUEUE = "/queue/feedback";
    }
}
