package com.graduation.his.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 诊后反馈消息DTO
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class FeedbackMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 诊断记录ID
     */
    private Long diagId;

    /**
     * 发送者类型(0-患者,1-医生)
     */
    private Integer senderType;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 发送者姓名
     */
    private String senderName;

    /**
     * 接收者ID
     */
    private Long receiverId;

    /**
     * 接收者姓名
     */
    private String receiverName;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 阅读状态(0-未读,1-已读)
     */
    private Integer readStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
} 