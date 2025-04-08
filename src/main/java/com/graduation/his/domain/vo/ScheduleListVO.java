package com.graduation.his.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排班列表简单VO，用于排班列表展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleListVO {
    
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
     * 医生简介
     */
    private String doctorIntroduction;
    
    /**
     * 医生头像URL
     */
    private String doctorAvatar;
    
    /**
     * 排班日期
     */
    private LocalDate scheduleDate;
    
    /**
     * 时间段(如 08:00-12:00)
     */
    private String timeSlot;
    
    /**
     * 可用剩余名额
     */
    private Integer remainingQuota;
    
    /**
     * 是否可预约
     */
    private Boolean canBook;
} 