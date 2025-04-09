package com.graduation.his.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduation.his.domain.po.FeedbackMessage;
import com.graduation.his.mapper.FeedbackMessageMapper;
import com.graduation.his.service.entity.IFeedbackMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 诊后反馈消息表 服务实现类
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Service
@Slf4j
public class FeedbackMessageServiceImpl extends ServiceImpl<FeedbackMessageMapper, FeedbackMessage> implements IFeedbackMessageService {

    @Override
    public List<FeedbackMessage> getMessagesByDiagId(Long diagId) {
        if (diagId == null) {
            log.error("诊断ID不能为空");
            return null;
        }
        
        LambdaQueryWrapper<FeedbackMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeedbackMessage::getDiagId, diagId)
               .orderByAsc(FeedbackMessage::getCreateTime);
        
        return this.list(wrapper);
    }

    @Override
    public Integer getUnreadCountForDoctor(Long doctorId) {
        if(doctorId == null) {
            log.error("医生ID不能为空");
            return 0;
        }
        
        // 医生作为接收者时，发送者类型是患者(senderType=2)
        LambdaQueryWrapper<FeedbackMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeedbackMessage::getSenderType, 2) // 2表示患者
               .eq(FeedbackMessage::getReadStatus, 0)  // 0表示未读
               .exists("SELECT 1 FROM diagnosis d WHERE d.diag_id = feedback_message.diag_id AND d.doctor_id = {0}", doctorId);
        
        return Math.toIntExact(this.count(wrapper));
    }

    @Override
    public Integer getUnreadCountForPatient(Long patientId) {
        if(patientId == null) {
            log.error("患者ID不能为空");
            return 0;
        }
        
        // 患者作为接收者时，发送者类型是医生(senderType=1)
        LambdaQueryWrapper<FeedbackMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeedbackMessage::getSenderType, 1) // 1表示医生
               .eq(FeedbackMessage::getReadStatus, 0)  // 0表示未读
               .exists("SELECT 1 FROM diagnosis d WHERE d.diag_id = feedback_message.diag_id AND d.patient_id = {0}", patientId);
        
        return Math.toIntExact(this.count(wrapper));
    }

    @Override
    public boolean markAsRead(Long messageId) {
        if (messageId == null) {
            log.error("消息ID不能为空");
            return false;
        }
        
        FeedbackMessage message = this.getById(messageId);
        if(message == null) {
            log.error("消息不存在，ID: {}", messageId);
            return false;
        }
        
        message.setReadStatus(1); // 1表示已读
        return this.updateById(message);
    }
    
    @Override
    public int getUnreadMessageCount(Long userId, Integer role) {
        if (userId == null || role == null) {
            log.error("用户ID或角色不能为空");
            return 0;
        }
        
        // 角色: 0-患者, 1-医生
        if (role == 0) {
            return getUnreadCountForPatient(userId);
        } else if (role == 1) {
            return getUnreadCountForDoctor(userId);
        } else {
            log.error("无效的角色类型: {}", role);
            return 0;
        }
    }
    
    @Override
    public boolean markAllAsRead(Long diagId, Long userId, Integer role) {
        if (diagId == null || userId == null || role == null) {
            log.error("诊断ID、用户ID或角色不能为空");
            return false;
        }
        
        LambdaUpdateWrapper<FeedbackMessage> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FeedbackMessage::getDiagId, diagId)
                    .eq(FeedbackMessage::getReadStatus, 0);
        
        // 根据角色确定需要标记的消息
        // 角色: 0-患者, 1-医生
        if (role == 0) {
            // 患者接收的消息由医生发送(senderType=1)
            updateWrapper.eq(FeedbackMessage::getSenderType, 1)
                        .exists("SELECT 1 FROM diagnosis d WHERE d.diag_id = feedback_message.diag_id AND d.patient_id = {0}", userId);
        } else if (role == 1) {
            // 医生接收的消息由患者发送(senderType=2)
            updateWrapper.eq(FeedbackMessage::getSenderType, 2)
                        .exists("SELECT 1 FROM diagnosis d WHERE d.diag_id = feedback_message.diag_id AND d.doctor_id = {0}", userId);
        } else {
            log.error("无效的角色类型: {}", role);
            return false;
        }
        
        FeedbackMessage entity = new FeedbackMessage();
        entity.setReadStatus(1); // 1表示已读
        
        return this.update(entity, updateWrapper);
    }
}
