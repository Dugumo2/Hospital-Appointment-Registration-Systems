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
 * 医院科室表
 * </p>
 *
 * @author hua
 * @since 2025-04-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("department")
public class Department implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 科室ID
     */
    @TableId(value = "dept_id", type = IdType.AUTO)
    private Long deptId;

    /**
     * 科室名称
     */
    private String deptName;

    /**
     * 科室编码
     */
    private String deptCode;

    /**
     * 科室类型(0-内科,1-外科,2-妇产科,3-儿科,4-五官科,5-其他)
     */
    private Integer deptType;

    /**
     * 科室描述
     */
    private String description;

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
