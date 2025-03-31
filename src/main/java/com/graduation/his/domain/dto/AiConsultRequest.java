package com.graduation.his.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI问诊请求DTO，用于接收前端的问诊请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConsultRequest {
    /**
     * 患者ID
     */
    private Long patientId;
    
    /**
     * 会话ID，首次对话为空
     */
    private String sessionId;
    
    /**
     * 预约ID(可选)
     */
    private Long appointmentId;
    
    /**
     * 用户问题内容
     */
    private String question;
} 