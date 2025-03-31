package com.graduation.his.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息记录DTO，用于存储对话中的单条消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRecord {
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息角色：system, user, assistant
     */
    private String role;
    
    /**
     * 消息创建时间
     */
    private LocalDateTime createTime;
} 