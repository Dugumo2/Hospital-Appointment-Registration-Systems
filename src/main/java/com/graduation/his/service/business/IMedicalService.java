package com.graduation.his.service.business;

import com.graduation.his.domain.dto.FeedbackMessageDTO;
import com.graduation.his.domain.po.Diagnosis;
import com.graduation.his.domain.po.FeedbackMessage;
import com.graduation.his.domain.po.User;
import com.graduation.his.domain.vo.DiagnosisVO;

import java.util.List;

/**
 * @author hua
 * @description 医疗服务接口
 * @create 2025-04-02 22:51
 */
public interface IMedicalService {
    
    /**
     * 获取患者的诊断记录列表
     * @param patientId 患者ID
     * @return 诊断记录VO列表
     */
    List<DiagnosisVO> getPatientDiagnoses(Long patientId);
    
    /**
     * 获取医生的诊断记录列表
     * @param doctorId 医生ID
     * @return 诊断记录VO列表
     */
    List<DiagnosisVO> getDoctorDiagnoses(Long doctorId);
    
    /**
     * 获取诊断详情
     * @param diagId 诊断ID
     * @return 诊断记录VO
     */
    DiagnosisVO getDiagnosisDetail(Long diagId);
    
    /**
     * 发送诊后反馈消息
     * @param diagId 诊断ID
     * @param content 消息内容
     * @param senderType 发送者类型(0-患者,1-医生)
     * @param senderId 发送者ID
     * @return 消息DTO
     */
    FeedbackMessageDTO sendFeedbackMessage(Long diagId, String content, Integer senderType, Long senderId);
    
    /**
     * 获取诊断相关的所有反馈消息
     * @param diagId 诊断ID
     * @return 消息DTO列表
     */
    List<FeedbackMessageDTO> getFeedbackMessages(Long diagId);
    
    /**
     * 标记消息为已读
     * @param messageId 消息ID
     * @return 是否成功
     */
    boolean markMessageAsRead(Long messageId);
    
    /**
     * 标记诊断相关的所有消息为已读
     * @param diagId 诊断ID
     * @param userId 用户ID
     * @param role 用户角色(0-患者,1-医生)
     * @return 是否成功
     */
    boolean markAllMessagesAsRead(Long diagId, Long userId, Integer role);
    
    /**
     * 获取用户的未读消息数量
     * @param userId 用户ID
     * @param role 用户角色(0-患者,1-医生)
     * @return 未读消息数量
     */
    int getUnreadMessageCount(Long userId, Integer role);
    
    /**
     * 检查诊断是否可以进行反馈(15天内)
     * @param diagId 诊断ID
     * @return 是否可以反馈
     */
    boolean canFeedback(Long diagId);

    /**
     * 根据用户ID获取患者ID
     * @param userId 用户ID
     * @return 患者ID
     */
    Long getPatientIdByUserId(Long userId);
    
    /**
     * 根据用户ID获取医生ID
     * @param userId 用户ID
     * @return 医生ID
     */
    Long getDoctorIdByUserId(Long userId);
    
    /**
     * 获取当前登录用户
     * @return 当前用户
     */
    User getCurrentUser();
    
    /**
     * 判断当前用户是否为指定患者
     * @param patientId 患者ID
     * @return 是否为当前患者
     */
    boolean isCurrentPatient(Long patientId);
    
    /**
     * 判断当前用户是否为指定医生
     * @param doctorId 医生ID
     * @return 是否为当前医生
     */
    boolean isCurrentDoctor(Long doctorId);
    
    /**
     * 异步发送消息到RabbitMQ
     * @param exchange 交换机
     * @param routingKey 路由键
     * @param message 消息
     */
    void sendToRabbitMQAsync(String exchange, String routingKey, Object message);
    
    /**
     * 异步更新用户未读消息数量
     * @param userId 用户ID
     * @param count 增加的数量
     */
    void updateUnreadMessageCountAsync(Long userId, int count);
}
