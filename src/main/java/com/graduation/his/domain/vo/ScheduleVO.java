package com.graduation.his.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 医生排班信息VO，用于接口返回
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleVO {
    
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
     * 排班状态(0-无效,1-有效)
     */
    private Integer status;
    
    /**
     * 是否可预约
     */
    private Boolean canBook;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
} 