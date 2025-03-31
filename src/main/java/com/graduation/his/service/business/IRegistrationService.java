package com.graduation.his.service.business;

import com.graduation.his.domain.dto.AiConsultRequest;
import com.graduation.his.domain.dto.ConsultSession;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author hua
 * @description 预约挂号与问诊服务接口
 * @create 2025-03-30 17:28
 */
public interface IRegistrationService {
    
    /**
     * 创建AI问诊SSE连接
     * @param sessionId 会话ID，首次对话为null
     * @return SSE连接对象
     */
    SseEmitter createAiConsultConnection(String sessionId);
    
    /**
     * 发送AI问诊请求
     * @param request 问诊请求
     * @return 会话ID
     */
    String sendAiConsultRequest(AiConsultRequest request);
    
    /**
     * 结束AI问诊会话（将会话保存到数据库并标记为已结束）
     * @param sessionId 会话ID
     * @return 是否成功
     */
    boolean endAiConsultSession(String sessionId);
    
    /**
     * 获取AI问诊历史会话（优先从Redis获取，Redis不存在则从数据库获取）
     * @param sessionId 会话ID
     * @return 会话详情
     */
    ConsultSession getAiConsultHistory(String sessionId);
}
