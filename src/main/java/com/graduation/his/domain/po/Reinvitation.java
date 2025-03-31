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
 * 复诊邀请表
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("reinvitation")
public class Reinvitation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 邀请ID
     */
    @TableId(value = "invite_id", type = IdType.AUTO)
    private Long inviteId;

    /**
     * 关联的诊断记录ID
     */
    private Long diagId;

    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 邀请消息
     */
    private String message;

    /**
     * 状态(0-未处理,1-已接受,2-已拒绝)
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
