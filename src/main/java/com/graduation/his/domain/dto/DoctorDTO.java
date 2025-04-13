package com.graduation.his.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * 医生DTO，用于创建和更新医生信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDTO {
    
    /**
     * 医生ID（更新时需要）
     */
    private Long doctorId;
    
    /**
     * 用户ID（更新时需要）
     */
    private Long userId;
    
    /**
     * 用户名/账号
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
     * 手机号
     */
    private String phone;
    
    /**
     * 医生姓名
     */
    private String name;
    
    /**
     * 所属门诊ID
     */
    private Long clinicId;
    
    /**
     * 职称(主任医师,副主任医师等)
     */
    private String title;
    
    /**
     * 医生简介
     */
    private String introduction;
    
    /**
     * 头像文件（创建/更新头像时使用）
     */
    private MultipartFile avatarFile;
    
    /**
     * 头像URL（仅在更新时可能使用，返回给前端）
     */
    private String avatar;
} 