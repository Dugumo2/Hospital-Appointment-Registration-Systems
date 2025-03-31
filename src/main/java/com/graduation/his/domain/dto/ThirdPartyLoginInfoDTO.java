package com.graduation.his.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 第三方登录补充信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThirdPartyLoginInfoDTO {
    
    /**
     * 第三方认证Token
     */
    private String thirdPartyToken;
    
    /**
     * 第三方平台用户ID
     */
    private String thirdPartyUserId;
    
    /**
     * 第三方平台类型（qq、weixin等）
     */
    private String platform;
    
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
     * 邮箱
     */
    private String email;
    
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