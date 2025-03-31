package com.graduation.his.service.business.impl;

import com.graduation.his.domain.dto.AiConsultConnectionRequest;
import com.graduation.his.domain.dto.AiConsultRequest;
import com.graduation.his.domain.dto.ConsultSession;
import com.graduation.his.service.business.IRegistrationService;
import com.graduation.his.service.entity.IAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author hua
 * @description 预约挂号与问诊服务实现类
 * @create 2025-03-30 17:29
 */
@Slf4j
@Service
public class RegistrationServiceImpl implements IRegistrationService {

    @Autowired
    private IAIService aiService;
    
    @Override
    public SseEmitter createAiConsultConnection(AiConsultConnectionRequest request) {
        log.info("创建AI问诊SSE连接, appointmentId: {}, patientId: {}, sessionId: {}", 
                request.getAppointmentId(), request.getPatientId(), request.getSessionId());
        
        // 验证参数
        if (request.getAppointmentId() == null) {
            throw new IllegalArgumentException("预约ID不能为空");
        }
        if (request.getPatientId() == null) {
            throw new IllegalArgumentException("患者ID不能为空");
        }
        
        // 调用AI服务创建连接
        return aiService.createSseConnection(request);
    }
    
    @Override
    public String sendAiConsultRequest(AiConsultRequest request) {
        log.info("发送AI问诊请求, patientId: {}, sessionId: {}", request.getPatientId(), request.getSessionId());
        
        // 参数验证
        if (request.getPatientId() == null) {
            throw new IllegalArgumentException("患者ID不能为空");
        }
        
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            throw new IllegalArgumentException("问题内容不能为空");
        }
        
        // 调用AI服务处理问诊请求
        String sessionId = aiService.processAiConsult(request);
        log.info("AI问诊请求已处理, 返回sessionId: {}", sessionId);
        
        return sessionId;
    }
    
    @Override
    public boolean endAiConsultSession(String sessionId) {
        log.info("结束AI问诊会话, sessionId: {}", sessionId);
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
        
        // 调用AI服务结束会话（会将会话保存到数据库中）
        boolean result = aiService.endConsultSession(sessionId);
        log.info("AI问诊会话结束 {}, sessionId: {}", result ? "成功" : "失败", sessionId);
        
        return result;
    }
    
    @Override
    public ConsultSession getAiConsultHistory(String sessionId) {
        log.info("获取AI问诊历史会话, sessionId: {}", sessionId);
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
        
        // 调用AI服务获取会话历史（优先从Redis获取，如Redis不存在则从数据库获取）
        ConsultSession session = aiService.getConsultSession(sessionId);
        
        if (session == null) {
            log.warn("未找到AI问诊历史会话, sessionId: {}", sessionId);
        } else {
            log.info("获取AI问诊历史会话成功, sessionId: {}, 消息数量: {}", 
                    sessionId, session.getMessageHistory().size());
        }
        
        return session;
    }
}
