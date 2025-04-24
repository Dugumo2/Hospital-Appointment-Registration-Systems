package com.graduation.his.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 患者ID
     */
    private Long patientId;
    
    /**
     * 医生ID
     */
    private Long doctorId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 头像URL
     */
    private String avatar;
    
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
     * 身份证号(脱敏显示)
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
    
    /**
     * 用户角色(0-患者,1-医生,2-管理员)
     */
    private Integer role;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 用户登录令牌
     */
    private String token;
} 