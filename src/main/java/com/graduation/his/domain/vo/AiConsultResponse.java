package com.graduation.his.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI问诊响应VO，用于前端展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConsultResponse {
    /**
     * 事件类型：message-消息, complete-完成, error-错误
     */
    private String event;
    
    /**
     * 角色：user-用户, assistant-AI
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 错误信息
     */
    private String error;
} 