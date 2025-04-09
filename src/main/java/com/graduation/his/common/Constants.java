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
         * 队列
         */
        public static final String FEEDBACK_MESSAGE_QUEUE = "hros.topic";
    }
}
