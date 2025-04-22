package com.graduation.his.utils.security;

import java.security.Principal;

/**
 * SaToken的Principal实现，用于WebSocket用户身份识别
 * @author hua
 */
public class StpPrincipal implements Principal {
    
    private final String loginId;
    
    public StpPrincipal(String loginId) {
        this.loginId = loginId;
    }
    
    @Override
    public String getName() {
        return this.loginId;
    }
    
    /**
     * 获取登录ID
     * @return 登录ID
     */
    public String getLoginId() {
        return this.loginId;
    }
} 