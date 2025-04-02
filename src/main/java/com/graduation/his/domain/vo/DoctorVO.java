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
     * 科室ID
     */
    private Integer deptId;
    
    /**
     * 科室名称
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
     * 头像URL
     */
    private String avatar;
    
    /**
     * 平均评分
     */
    private Double avgRating;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
} 