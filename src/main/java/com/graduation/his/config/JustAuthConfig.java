package com.graduation.his.config;

import me.zhyd.oauth.cache.AuthStateCache;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.request.AuthQqRequest;
import me.zhyd.oauth.request.AuthRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * JustAuth第三方登录配置
 */
@Configuration
public class JustAuthConfig {

    @Value("${justauth.qq.client-id}")
    private String qqClientId;

    @Value("${justauth.qq.client-secret}")
    private String qqClientSecret;

    @Value("${justauth.qq.redirect-uri}")
    private String qqRedirectUri;

    /**
     * 创建QQ认证请求对象
     */
    @Bean
    public AuthRequest qqAuthRequest(AuthStateCache authStateCache) {
        return new AuthQqRequest(AuthConfig.builder()
                .clientId(qqClientId)
                .clientSecret(qqClientSecret)
                .redirectUri(qqRedirectUri)
                .build(), authStateCache);
    }

    /**
     * 使用Redis缓存Auth state
     */
    @Bean
    public AuthStateCache authStateCache(StringRedisTemplate stringRedisTemplate) {
        return new RedisAuthStateCache(stringRedisTemplate);
    }

    /**
     * 提供所有支持的第三方登录请求
     */
    @Bean
    public Map<String, AuthRequest> authRequestMap(AuthRequest qqAuthRequest) {
        Map<String, AuthRequest> map = new HashMap<>();
        map.put("qq", qqAuthRequest);
        return map;
    }

    /**
     * Redis实现的AuthStateCache
     */
    private static class RedisAuthStateCache implements AuthStateCache {
        private final StringRedisTemplate redisTemplate;
        private static final String KEY_PREFIX = "auth:state:";
        private static final long DEFAULT_EXPIRE_TIME = 300L; // 5分钟过期

        public RedisAuthStateCache(StringRedisTemplate redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        @Override
        public void cache(String key, String value) {
            redisTemplate.opsForValue().set(KEY_PREFIX + key, value, java.time.Duration.ofSeconds(DEFAULT_EXPIRE_TIME));
        }
        
        @Override
        public void cache(String key, String value, long timeout) {
            redisTemplate.opsForValue().set(KEY_PREFIX + key, value, java.time.Duration.ofSeconds(timeout));
        }

        @Override
        public String get(String key) {
            return redisTemplate.opsForValue().get(KEY_PREFIX + key);
        }

        @Override
        public boolean containsKey(String key) {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + key));
        }
    }
} 