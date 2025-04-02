package com.graduation.his.config;

import com.graduation.his.utils.redis.IRedisService;
import me.zhyd.oauth.cache.AuthStateCache;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.request.AuthQqRequest;
import me.zhyd.oauth.request.AuthRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * JustAuth第三方登录配置
 * 目前支持平台：QQ
 * QQ登录采用OAuth2.0的Authorization Code模式
 */
@Configuration
@Slf4j
public class JustAuthConfig {

    @Value("${oauth.qq.app-id}")
    private String qqClientId;

    @Value("${oauth.qq.app-key}")
    private String qqClientSecret;

    @Value("${oauth.qq.redirect-uri}")
    private String qqRedirectUri;

    private final IRedisService redisService;
    
    private static final String AUTH_STATE_KEY_PREFIX = "auth:state:";
    private static final long AUTH_STATE_EXPIRE_TIME = 300L; // 5分钟过期

    public JustAuthConfig(IRedisService redisService) {
        this.redisService = redisService;
        log.info("JustAuthConfig初始化: redisService是否为null: {}", redisService == null);
    }

    /**
     * 使用Redis缓存Auth state
     */
    @Bean
    public AuthStateCache authStateCache() {
        log.info("创建authStateCache");
        return new AuthStateCache() {
            @Override
            public void cache(String key, String value) {
                redisService.setValue(AUTH_STATE_KEY_PREFIX + key, value, TimeUnit.SECONDS.toMillis(AUTH_STATE_EXPIRE_TIME));
            }
            
            @Override
            public void cache(String key, String value, long timeout) {
                redisService.setValue(AUTH_STATE_KEY_PREFIX + key, value, TimeUnit.SECONDS.toMillis(timeout));
            }

            @Override
            public String get(String key) {
                return redisService.getValue(AUTH_STATE_KEY_PREFIX + key);
            }

            @Override
            public boolean containsKey(String key) {
                return redisService.isExists(AUTH_STATE_KEY_PREFIX + key);
            }
        };
    }

    /**
     * 创建QQ认证请求对象
     */
    @Bean(name = "qq")
    public AuthRequest qqAuthRequest() {
        log.info("创建QQ认证请求对象: QQ参数 - clientId: {}, clientSecret: {}, redirectUri: {}", 
                qqClientId, 
                qqClientSecret != null ? "已设置" : "未设置", 
                qqRedirectUri);
        
        AuthStateCache stateCache = authStateCache();
        return new AuthQqRequest(AuthConfig.builder()
                .clientId(qqClientId)
                .clientSecret(qqClientSecret)
                .redirectUri(qqRedirectUri)
                .build(), stateCache);
    }

    /**
     * 提供所有支持的第三方登录请求
     */
    @Bean
    public Map<String, AuthRequest> authRequestMap() {
        log.info("创建authRequestMap");
        Map<String, AuthRequest> map = new HashMap<>();
        
        // 确保QQ认证请求对象被正确创建
        AuthRequest qqRequest = qqAuthRequest();
        map.put("qq", qqRequest);
        log.info("QQ认证请求对象已添加到map: {}", qqRequest != null);
        
        return map;
    }
} 