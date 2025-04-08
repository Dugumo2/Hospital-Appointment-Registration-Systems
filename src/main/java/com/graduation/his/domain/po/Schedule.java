package com.graduation.his.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 医生排班表
 * </p>
 *
 * @author hua
 * @since 2025-04-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("schedule")
public class Schedule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 排班ID
     */
    @TableId(value = "schedule_id", type = IdType.AUTO)
    private Long scheduleId;

    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 门诊ID
     */
    private Long clinicId;

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
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
