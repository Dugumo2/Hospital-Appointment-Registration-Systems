package com.graduation.his.controller;

import com.graduation.his.domain.dto.AiConsultConnectionRequest;
import com.graduation.his.domain.dto.AiConsultRequest;
import com.graduation.his.domain.dto.ConsultSession;
import com.graduation.his.domain.vo.Result;
import com.graduation.his.service.business.IRegistrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <p>
 * 预约挂号表 前端控制器
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Slf4j
@RestController
@RequestMapping("/appointment")
public class RegistrationController {
    
    @Autowired
    private IRegistrationService registrationService;
    
    /**
     * 创建AI问诊SSE连接
     * 
     * 创建Server-Sent Events连接，用于实时接收AI问诊响应
     * 会话状态会存储在Redis中，有效期为6小时
     * 
     * @param request 连接请求(包含会话ID、预约ID和患者ID)
     * @return SSE连接
     */
    @PostMapping(value = "/ai-consult/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter createAiConsultConnection(@RequestBody AiConsultConnectionRequest request) {
        log.info("接收到创建AI问诊SSE连接请求, appointmentId: {}, patientId: {}, sessionId: {}", 
                request.getAppointmentId(), request.getPatientId(), request.getSessionId());
        
        try {
            return registrationService.createAiConsultConnection(request);
        } catch (IllegalArgumentException e) {
            log.error("创建AI问诊SSE连接参数错误: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("创建AI问诊SSE连接异常", e);
            throw new RuntimeException("服务异常，请稍后重试");
        }
    }
    
    /**
     * 发送AI问诊请求
     * 
     * 向AI发送问诊请求，会话进行中时会话状态存储在Redis中
     * 每条用户消息和AI回复都会实时记录到Redis，有效期为6小时
     * 
     * @param request 问诊请求
     * @return 结果(包含会话ID)
     */
    @PostMapping("/ai-consult/send")
    public Result<String> sendAiConsultRequest(@RequestBody AiConsultRequest request) {
        log.info("接收到AI问诊请求, patientId: {}, sessionId: {}", request.getPatientId(), request.getSessionId());
        try {
            String sessionId = registrationService.sendAiConsultRequest(request);
            return Result.ok(sessionId, "AI问诊请求已发送");
        } catch (IllegalArgumentException e) {
            log.error("AI问诊请求参数错误: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("AI问诊请求异常", e);
            return Result.fail("服务异常，请稍后重试");
        }
    }
    
    /**
     * 结束AI问诊会话
     * 
     * 结束会话并将完整对话历史保存到数据库中
     * 此操作会将会话状态标记为已结束，并永久保留对话历史
     * 
     * @param sessionId 会话ID
     * @return 结果
     */
    @PostMapping("/ai-consult/end")
    public Result<Boolean> endAiConsultSession(
            @RequestParam String sessionId) {
        log.info("接收到结束AI问诊会话请求, sessionId: {}", sessionId);
        try {
            boolean result = registrationService.endAiConsultSession(sessionId);
            return Result.ok(result, result ? "会话已结束" : "结束会话失败");
        } catch (IllegalArgumentException e) {
            log.error("结束AI问诊会话参数错误: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("结束AI问诊会话异常", e);
            return Result.fail("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取AI问诊历史会话
     * 
     * 获取历史会话详情，包括所有对话内容
     * 优先从Redis获取活跃会话，若Redis中不存在则从数据库获取已结束会话
     * 
     * @param sessionId 会话ID
     * @return 会话详情
     */
    @GetMapping("/ai-consult/history")
    public Result<ConsultSession> getAiConsultHistory(
            @RequestParam String sessionId) {
        log.info("接收到获取AI问诊历史会话请求, sessionId: {}", sessionId);
        try {
            ConsultSession session = registrationService.getAiConsultHistory(sessionId);
            if (session == null) {
                return Result.fail("未找到相关会话记录");
            }
            return Result.ok(session, "获取会话记录成功");
        } catch (IllegalArgumentException e) {
            log.error("获取AI问诊历史会话参数错误: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("获取AI问诊历史会话异常", e);
            return Result.fail("服务异常，请稍后重试");
        }
    }
}
