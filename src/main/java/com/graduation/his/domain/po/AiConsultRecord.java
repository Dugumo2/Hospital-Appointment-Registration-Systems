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
 * AI 问诊记录表
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ai_consult_record")
public class AiConsultRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 问诊记录ID
     */
    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;

    /**
     * 关联的预约ID
     */
    private Long appointmentId;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * AI 对话内容(可存储 JSON)
     */
    private String conversation;

    /**
     * 问诊状态(0-进行中,1-已结束)
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
