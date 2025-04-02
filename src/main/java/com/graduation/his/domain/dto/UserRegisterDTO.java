package com.graduation.his.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注册DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterDTO {
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 邮箱验证码
     */
    private String verifyCode;
    
    /**
     * 患者姓名
     */
    private String name;
    
    /**
     * 性别（0-未知,1-男,2-女）
     */
    private Integer gender;
    
    /**
     * 年龄
     */
    private Integer age;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 身份证号
     */
    private String idCard;
    
    /**
     * 地区(省市区)
     */
    private String region;
    
    /**
     * 详细住址
     */
    private String address;
} 