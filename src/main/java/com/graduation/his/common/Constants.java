package com.graduation.his.common;

public class Constants {
    public static class RedisKey {
        // AI会话前缀
        public static String AI_CONSULT_SESSION = "ai_consult:session:";

        // Redis分布式锁前缀
        public static String AI_CONSULT_LOCK = "ai_consult:lock:";
    }
}
