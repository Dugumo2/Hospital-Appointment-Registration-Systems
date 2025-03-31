package com.graduation.his.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 医生诊断记录表
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("diagnosis")
public class Diagnosis implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 诊断记录ID
     */
    @TableId(value = "diag_id", type = IdType.AUTO)
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
     */
    private String prescription;

    /**
     * 医嘱
     */
    private String advice;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
