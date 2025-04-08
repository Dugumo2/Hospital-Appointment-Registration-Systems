package com.graduation.his.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排班详情VO，用于预约页面展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDetailVO {
    
    /**
     * 排班ID
     */
    private Long scheduleId;
    
    /**
     * 医生ID
     */
    private Long doctorId;
    
    /**
     * 医生姓名
     */
    private String doctorName;
    
    /**
     * 医生职称
     */
    private String doctorTitle;
    
    /**
     * 医生头像URL
     */
    private String doctorAvatar;
    
    /**
     * 科室ID
     */
    private Long deptId;
    
    /**
     * 科室名称
     */
    private String deptName;
    
    /**
     * 门诊ID
     */
    private Long clinicId;
    
    /**
     * 门诊名称
     */
    private String clinicName;
    
    /**
     * 排班日期
     */
    private LocalDate scheduleDate;
    
    /**
     * 时间段(如 08:00-12:00)
     */
    private String timeSlot;
    
    /**
     * 该时段可挂号最大人数
     */
    private Integer maxPatients;
    
    /**
     * 当前已预约人数
     */
    private Integer currentPatients;
    
    /**
     * 剩余可预约数量
     */
    private Integer remainingQuota;
    
    /**
     * 是否可预约
     */
    private Boolean canBook;
    
    /**
     * 患者姓名（当前登录用户关联的患者，可选）
     */
    private String patientName;
    
    /**
     * 患者ID（当前登录用户关联的患者，可选）
     */
    private Long patientId;
} 