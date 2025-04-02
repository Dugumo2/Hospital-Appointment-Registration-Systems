package com.graduation.his.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约挂号信息VO，用于接口返回
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentVO {
    
    /**
     * 预约ID
     */
    private Long appointmentId;
    
    /**
     * 患者ID
     */
    private Long patientId;
    
    /**
     * 患者姓名
     */
    private String patientName;
    
    /**
     * 医生ID
     */
    private Long doctorId;
    
    /**
     * 医生姓名
     */
    private String doctorName;
    
    /**
     * 科室ID
     */
    private Integer deptId;
    
    /**
     * 科室名称
     */
    private String deptName;
    
    /**
     * 排班ID
     */
    private Long scheduleId;
    
    /**
     * 预约日期
     */
    private LocalDate appointmentDate;
    
    /**
     * 预约时间段
     */
    private String timeSlot;
    
    /**
     * 预约状态(0-待就诊,1-已就诊,2-已取消等)
     */
    private Integer status;
    
    /**
     * 预约状态描述
     */
    private String statusDesc;
    
    /**
     * 是否允许取消
     */
    private Boolean canCancel;
    
    /**
     * AI问诊会话ID
     */
    private String aiConsultSessionId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
} 