package com.graduation.his.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 诊断记录VO，用于前端展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisVO {
    
    /**
     * 诊断记录ID
     */
    private Long diagId;
    
    /**
     * 预约ID
     */
    private Long appointmentId;
    
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
     * 患者ID
     */
    private Long patientId;
    
    /**
     * 患者姓名
     */
    private String patientName;
    
    /**
     * 诊断结果
     */
    private String diagnosisResult;
    
    /**
     * 检查记录
     */
    private String examination;
    
    /**
     * 处方信息
     */
    private String prescription;
    
    /**
     * 医嘱
     */
    private String advice;
    
    /**
     * 是否可以进行诊后反馈(在诊断后15天内)
     */
    private Boolean canFeedback;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
} 