package com.graduation.his.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 医生信息VO，用于接口返回
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorVO {
    
    /**
     * 医生ID
     */
    private Long doctorId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 医生姓名
     */
    private String name;
    
    /**
     * 门诊ID
     */
    private Long clinicId;
    
    /**
     * 科室名称 (非实体字段，用于前端展示)
     */
    private String deptName;
    
    /**
     * 职称
     */
    private String title;
    
    /**
     * 医生简介
     */
    private String introduction;
    
    /**
     * 头像URL (非实体字段，用于前端展示)
     */
    private String avatar;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
} 