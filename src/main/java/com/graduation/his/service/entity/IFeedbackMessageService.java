package com.graduation.his.service.entity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduation.his.domain.dto.FeedbackMessageDTO;
import com.graduation.his.domain.po.FeedbackMessage;

import java.util.List;

/**
 * <p>
 * 诊后反馈消息服务接口
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
public interface IFeedbackMessageService extends IService<FeedbackMessage> {

    /**
     * 根据诊断ID获取反馈消息列表
     * @param diagId 诊断ID
     * @return 反馈消息列表
     */
    List<FeedbackMessage> getMessagesByDiagId(Long diagId);

    /**
     * 获取医生的未读消息数量
     * @param doctorId 医生ID
     * @return 未读消息数量
     */
    Integer getUnreadCountForDoctor(Long doctorId);

    /**
     * 获取患者的未读消息数量
     * @param patientId 患者ID
     * @return 未读消息数量
     */
    Integer getUnreadCountForPatient(Long patientId);

    /**
     * 将消息标记为已读
     * @param messageId 消息ID
     * @return 是否成功
     */
    boolean markAsRead(Long messageId);
    
    /**
     * 获取用户的未读消息数量
     * @param userId 用户ID
     * @param role 用户角色(0-患者,1-医生)
     * @return 未读消息数量
     */
    int getUnreadMessageCount(Long userId, Integer role);
    
    /**
     * 将诊断相关的所有消息标记为已读
     * @param diagId 诊断ID
     * @param userId 用户ID
     * @param role 用户角色(0-患者,1-医生)
     * @return 是否成功
     */
    boolean markAllAsRead(Long diagId, Long userId, Integer role);

    /**
     * 获取接收者的未读消息列表
     * @param receiverId 接收者ID（患者ID或医生ID）
     * @param receiverType 接收者类型（0:患者，1:医生）
     * @return 未读消息列表
     */
    List<FeedbackMessageDTO> getPendingMessagesByReceiverId(Long receiverId, Integer receiverType);
}
