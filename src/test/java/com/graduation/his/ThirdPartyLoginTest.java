package com.graduation.his;

import com.graduation.his.config.JustAuthConfig;
import com.graduation.his.service.business.IAuthService;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.request.AuthRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

/**
 * 第三方登录测试类
 */
@Slf4j
@SpringBootTest
public class ThirdPartyLoginTest {

    @Autowired
    private IAuthService authService;
    
    @Autowired
    private Map<String, AuthRequest> authRequestMap;
    
    @Autowired
    private JustAuthConfig justAuthConfig;
    
    @Value("${oauth.qq.app-id}")
    private String qqAppId;
    
    @Value("${oauth.qq.app-key}")
    private String qqAppKey;
    
    @Value("${oauth.qq.redirect-uri}")
    private String qqRedirectUri;

    /**
     * 测试配置是否正确读取
     */
    @Test
    public void testConfig() {
        log.info("========= 第三方登录配置测试 =========");
        log.info("QQ AppID: {}", qqAppId);
        log.info("QQ AppKey: {}", qqAppKey);
        log.info("QQ Redirect URI: {}", qqRedirectUri);
        log.info("==================================");
    }

    /**
     * 测试获取QQ登录URL
     * 运行此测试会在控制台输出QQ登录URL，复制到浏览器打开即可进行登录测试
     */
    @Test
    public void testGetQQLoginUrl() {
        String platform = "qq";
        
        // 检查authRequestMap
        log.info("========= 检查authRequestMap =========");
        log.info("authRequestMap是否为null: {}", (authRequestMap == null));
        if (authRequestMap != null) {
            log.info("authRequestMap大小: {}", authRequestMap.size());
            log.info("authRequestMap包含的key: {}", authRequestMap.keySet());
            log.info("authRequestMap.get(qq)是否为null: {}", (authRequestMap.get("qq") == null));
        }
        log.info("===================================");
        
        try {
            String loginUrl = authService.getThirdPartyLoginUrl(platform);
            
            log.info("========= 第三方登录测试 =========");
            log.info("平台：{}", platform);
            log.info("登录URL：{}", loginUrl);
            log.info("请将上面的URL复制到浏览器中打开，进行QQ登录授权测试");
            log.info("登录后会跳转到配置的回调地址: {}", "http://127.0.0.1:8080/api/auth/thirdParty/callback/qq");
            log.info("===============================");
        } catch (Exception e) {
            log.error("获取登录URL失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 测试已配置的AuthRequest是否正确
     */
    @Test
    public void testAuthRequestMap() {
        log.info("========= 第三方登录配置测试 =========");
        log.info("authRequestMap是否为null: {}", (authRequestMap == null));
        if (authRequestMap != null) {
            log.info("authRequestMap大小: {}", authRequestMap.size());
            log.info("authRequestMap包含的key: {}", authRequestMap.keySet());
            
            AuthRequest qqAuthRequest = authRequestMap.get("qq");
            if (qqAuthRequest != null) {
                log.info("QQ登录配置已正确加载");
            } else {
                log.error("QQ登录配置未正确加载");
                // 打印所有键，查看实际的键是什么
                if (!authRequestMap.isEmpty()) {
                    for (String key : authRequestMap.keySet()) {
                        log.info("实际键: '{}', 值类型: {}", key, authRequestMap.get(key).getClass().getName());
                    }
                }
            }
        } else {
            log.error("authRequestMap为null，请检查配置");
        }
        log.info("==================================");
    }
    
    /**
     * 模拟第三方登录回调
     * 注意：此测试方法无法直接运行，因为需要实际的QQ登录回调参数
     * 仅用于演示回调处理流程
     */
    @Test
    public void testThirdPartyCallback() {
        // 模拟AuthCallback参数
        // 实际情况下，这些参数由QQ登录回调提供
        AuthCallback callback = new AuthCallback();
        callback.setCode("authorization_code_from_qq");
        callback.setState("state_generated_in_authorize_step");
        
        // 此方法无法在测试环境中直接运行成功，因为需要真实的QQ回调
        // 实际使用中，是由前端页面跳转到QQ登录，然后QQ登录成功后回调到我们的接口
        try {
            log.info("模拟QQ登录回调处理...");
            String redirectUrl = authService.thirdPartyCallback("qq", callback);
            log.info("回调处理完成，重定向URL: {}", redirectUrl);
        } catch (Exception e) {
            log.error("回调处理异常: {}", e.getMessage());
            // 在测试环境中，这里预期会抛出异常，因为使用的是模拟数据
        }
    }
} 