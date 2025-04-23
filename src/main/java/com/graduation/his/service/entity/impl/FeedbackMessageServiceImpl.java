package com.graduation.his.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduation.his.domain.dto.FeedbackMessageDTO;
import com.graduation.his.domain.po.Doctor;
import com.graduation.his.domain.po.FeedbackMessage;
import com.graduation.his.domain.po.Patient;
import com.graduation.his.mapper.FeedbackMessageMapper;
import com.graduation.his.service.entity.IDoctorService;
import com.graduation.his.service.entity.IFeedbackMessageService;
import com.graduation.his.service.entity.IPatientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 诊断反馈消息表 服务实现类
 * </p>
 *
 * @author hua
 * @since 2025-04-03
 */
@Slf4j
@Service
public class FeedbackMessageServiceImpl extends ServiceImpl<FeedbackMessageMapper, FeedbackMessage> implements IFeedbackMessageService {

    @Autowired
    private IPatientService patientService;
    
    @Autowired
    private IDoctorService doctorService;

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
        
        // 医生作为接收者时，发送者类型是患者(senderType=0)
        LambdaQueryWrapper<FeedbackMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeedbackMessage::getSenderType, 0) // 0表示患者
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
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
    public boolean markAllAsRead(Long diagId, Long entityId, Integer role) {
        if (diagId == null || entityId == null || role == null) {
            log.error("诊断ID、实体ID或角色不能为空");
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
                        .exists("SELECT 1 FROM diagnosis d WHERE d.diag_id = feedback_message.diag_id AND d.patient_id = {0}", entityId);
        } else if (role == 1) {
            // 医生接收的消息由患者发送(senderType=0)
            updateWrapper.eq(FeedbackMessage::getSenderType, 0)
                        .exists("SELECT 1 FROM diagnosis d WHERE d.diag_id = feedback_message.diag_id AND d.doctor_id = {0}", entityId);
        } else {
            log.error("无效的角色类型: {}", role);
            return false;
        }
        
        FeedbackMessage entity = new FeedbackMessage();
        entity.setReadStatus(1); // 1表示已读
        
        return this.update(entity, updateWrapper);
    }

    @Override
    public List<FeedbackMessageDTO> getPendingMessagesByReceiverId(Long receiverId, Integer receiverType) {
        if (receiverId == null || receiverType == null) {
            log.error("接收者ID或类型不能为空");
            return new ArrayList<>();
        }
        
        // receiverType表示接收者的类型，与senderType相反
        // 如果receiverType=0，表示接收者是患者，则查询医生发送的消息(senderType=1)
        // 如果receiverType=1，表示接收者是医生，则查询患者发送的消息(senderType=0)
        int senderType = receiverType == 0 ? 1 : 0;
        
        // 查询特定用户的未读消息
        List<FeedbackMessage> messages;
        if (receiverType == 0) {
            // 患者
            LambdaQueryWrapper<FeedbackMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FeedbackMessage::getSenderType, 1) // 医生发送的
                   .eq(FeedbackMessage::getReadStatus, 0) // 未读
                   .exists("SELECT 1 FROM diagnosis d WHERE d.diag_id = feedback_message.diag_id AND d.patient_id = {0}", receiverId)
                   .orderByAsc(FeedbackMessage::getCreateTime);
            messages = list(wrapper);
        } else {
            // 医生
            LambdaQueryWrapper<FeedbackMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FeedbackMessage::getSenderType, 0) // 患者发送的
                   .eq(FeedbackMessage::getReadStatus, 0) // 未读
                   .exists("SELECT 1 FROM diagnosis d WHERE d.diag_id = feedback_message.diag_id AND d.doctor_id = {0}", receiverId)
                   .orderByAsc(FeedbackMessage::getCreateTime);
            messages = list(wrapper);
        }
        
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取所有涉及的诊断ID
        List<Long> diagIds = messages.stream()
                .map(FeedbackMessage::getDiagId)
                .distinct()
                .collect(Collectors.toList());
        
        // 获取所有发送者ID
        List<Long> senderIds = messages.stream()
                .map(FeedbackMessage::getSenderId)
                .distinct()
                .collect(Collectors.toList());
        
        // 获取发送者信息
        Map<Long, String> senderNameMap;
        if (senderType == 0) {
            // 发送者是患者
            senderNameMap = patientService.listByIds(senderIds).stream()
                    .collect(Collectors.toMap(
                            Patient::getPatientId,
                            Patient::getName,
                            (v1, v2) -> v1));
        } else {
            // 发送者是医生
            senderNameMap = doctorService.listByIds(senderIds).stream()
                    .collect(Collectors.toMap(
                            Doctor::getDoctorId,
                            Doctor::getName,
                            (v1, v2) -> v1));
        }
        
        // 转换为DTO
        List<FeedbackMessageDTO> dtoList = new ArrayList<>(messages.size());
        for (FeedbackMessage message : messages) {
            FeedbackMessageDTO dto = new FeedbackMessageDTO();
            BeanUtils.copyProperties(message, dto);
            
            // 设置发送者姓名（如果原始数据中没有）
            if (dto.getSenderName() == null || dto.getSenderName().isEmpty()) {
                String senderName = senderNameMap.getOrDefault(message.getSenderId(), "未知");
                dto.setSenderName(senderName);
            }
            
            // 设置接收者ID和姓名
            dto.setReceiverId(receiverId);
            dto.setReceiverName(receiverType == 0 ? "患者" : "医生");
            
            dtoList.add(dto);
        }
        
        return dtoList;
    }

    /**
     * 获取特定诊断的未读消息数量
     * @param diagId 诊断ID
     * @param entityId 实体ID（患者ID或医生ID）
     * @param role 用户角色(0-患者,1-医生)
     * @return 未读消息数量
     */
    @Override
    public int getUnreadMessageCountByDiag(Long diagId, Long entityId, Integer role) {
        log.info("获取特定诊断的未读消息数量, diagId: {}, entityId: {}, role: {}", diagId, entityId, role);
        
        // 构建查询条件
        LambdaQueryWrapper<FeedbackMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeedbackMessage::getDiagId, diagId); // 诊断ID相同
        wrapper.eq(FeedbackMessage::getReadStatus, 0); // 未读状态
        
        // 根据角色判断接收者条件
        // 患者接收的消息是医生发送的(senderType=1)，医生接收的消息是患者发送的(senderType=0)
        int receiverType = (role == 0) ? 1 : 0; // 角色的反向就是发送者类型
        wrapper.eq(FeedbackMessage::getSenderType, receiverType);
        
        // 返回数量
        return (int)count(wrapper);
    }

    /**
     * 获取指定实体的所有未读消息数量映射
     * @param entityId 实体ID（患者ID或医生ID）
     * @param role 用户角色(0-患者,1-医生)
     * @return 诊断ID -> 未读消息数量 的映射
     */
    @Override
    public Map<String, Integer> getUnreadMessageCountsByEntityId(Long entityId, Integer role) {
        log.info("获取实体的所有未读消息数量映射, entityId: {}, role: {}", entityId, role);
        
        if (entityId == null || role == null) {
            log.warn("参数错误: entityId={}, role={}", entityId, role);
            return new HashMap<>();
        }
        
        try {
            // 查询条件
            LambdaQueryWrapper<FeedbackMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FeedbackMessage::getReadStatus, 0); // 未读状态
            
            // 根据角色类型确定查询条件
            if (role == 0) { // 患者
                // 患者接收的消息是医生发送的(senderType=1)
                wrapper.eq(FeedbackMessage::getSenderType, 1)
                       .exists("SELECT 1 FROM diagnosis d WHERE d.diag_id = feedback_message.diag_id AND d.patient_id = {0}", entityId);
            } else if (role == 1) { // 医生
                // 医生接收的消息是患者发送的(senderType=0)
                wrapper.eq(FeedbackMessage::getSenderType, 0)
                       .exists("SELECT 1 FROM diagnosis d WHERE d.diag_id = feedback_message.diag_id AND d.doctor_id = {0}", entityId);
            } else {
                log.error("无效的角色类型: {}", role);
                return new HashMap<>();
            }
            
            // 查询所有未读消息
            List<FeedbackMessage> messages = list(wrapper);
            
            // 按诊断ID分组计数
            Map<String, Integer> result = new HashMap<>();
            for (FeedbackMessage message : messages) {
                String diagId = message.getDiagId().toString();
                result.put(diagId, result.getOrDefault(diagId, 0) + 1);
            }
            
            return result;
        } catch (Exception e) {
            log.error("获取未读消息数量映射异常", e);
            return new HashMap<>();
        }
    }

    /**
     * 获取用户的未读消息总数
     * @param entityId 实体ID（患者ID或医生ID）
     * @param role 用户角色(0-患者,1-医生)
     * @return 未读消息总数
     */
    @Override
    public int getUnreadMessageCount(Long entityId, Integer role) {
        log.info("获取实体的所有未读消息总数, entityId: {}, role: {}", entityId, role);
        
        if (entityId == null || role == null) {
            log.warn("参数错误: entityId={}, role={}", entityId, role);
            return 0;
        }
        
        // 查询条件
        LambdaQueryWrapper<FeedbackMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeedbackMessage::getReadStatus, 0); // 未读状态
        
        // 根据角色类型确定查询条件
        if (role == 0) { // 患者
            // 患者接收的消息是医生发送的(senderType=1)
            wrapper.eq(FeedbackMessage::getSenderType, 1)
                    .exists("SELECT 1 FROM diagnosis d WHERE d.diag_id = feedback_message.diag_id AND d.patient_id = {0}", entityId);
        } else if (role == 1) { // 医生
            // 医生接收的消息是患者发送的(senderType=0)
            wrapper.eq(FeedbackMessage::getSenderType, 0)
                    .exists("SELECT 1 FROM diagnosis d WHERE d.diag_id = feedback_message.diag_id AND d.doctor_id = {0}", entityId);
        } else {
            log.error("无效的角色类型: {}", role);
            return 0;
        }
        
        // 返回计数
        return (int)count(wrapper);
    }
}
