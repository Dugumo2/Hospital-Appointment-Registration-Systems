package com.graduation.his.domain.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 诊断记录数据传输对象
 * 注意：诊断记录一旦创建不可修改，这是医疗行业的标准规范
 */
@Data
public class DiagnosisDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 预约ID
     */
    private Long appointmentId;
    
    /**
     * 医生ID
     */
    private Long doctorId;
    
    /**
     * 患者ID
     */
    private Long patientId;
    
    /**
     * 诊断结果
     */
    private String diagnosisResult;
    
    /**
     * 检查记录
     */
    private String examination;
    
    /**
     * 处方信息(药品、数量、用法等)
     * 格式：药品名称,数量,单位,备注;药品名称,数量,单位,备注
     */
    private String prescription;
    
    /**
     * 医嘱
     */
    private String advice;
} 