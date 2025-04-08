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
 * 门诊表
 * </p>
 *
 * @author hua
 * @since 2025-04-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("clinic")
public class Clinic implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 门诊ID
     */
    @TableId(value = "clinic_id", type = IdType.AUTO)
    private Long clinicId;

    /**
     * 所属科室ID
     */
    private Long deptId;

    /**
     * 门诊名称
     */
    private String clinicName;

    /**
     * 是否有效(0-无效,1-有效)
     */
    private Integer isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
