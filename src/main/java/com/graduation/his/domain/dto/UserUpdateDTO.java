package com.graduation.his.domain.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 用户个人信息更新数据传输对象
 */
@Data
public class UserUpdateDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 姓名
     */
    private String name;
    
    /**
     * 性别（0-女，1-男）
     */
    private Integer gender;
    
    /**
     * 年龄
     */
    private Integer age;
    
    /**
     * 身份证号
     */
    private String idCard;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 地区
     */
    private String region;
    
    /**
     * 详细地址
     */
    private String address;
} 