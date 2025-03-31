package com.graduation.his.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginDTO {
    
    /**
     * 登录账号（用户名或邮箱）
     */
    private String account;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 是否记住我（默认为false）
     */
    private Boolean rememberMe = false;
} 