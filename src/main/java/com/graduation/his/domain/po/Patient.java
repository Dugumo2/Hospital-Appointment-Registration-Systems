package com.graduation.his.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * <p>
 * 患者信息表
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Data
@TableName("patient")
public class Patient {

    /**
     * 患者ID
     */
    @TableId(value = "patient_id", type = IdType.AUTO)
    private Long patientId;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 患者姓名
     */
    private String name;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 性别(0-未知,1-男,2-女)
     */
    private Integer gender;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 地区(省市区)
     */
    private String region;

    /**
     * 详细住址
     */
    private String address;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
