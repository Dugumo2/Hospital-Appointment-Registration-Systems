package com.graduation.his.service.business.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.graduation.his.common.Constants;
import com.graduation.his.domain.dto.FeedbackMessageDTO;
import com.graduation.his.domain.po.Diagnosis;
import com.graduation.his.domain.po.Doctor;
import com.graduation.his.domain.po.FeedbackMessage;
import com.graduation.his.domain.po.Patient;
import com.graduation.his.domain.po.User;
import com.graduation.his.domain.vo.DiagnosisVO;
import com.graduation.his.exception.BusinessException;
import com.graduation.his.service.business.IMedicalService;
import com.graduation.his.service.entity.*;
import com.graduation.his.utils.redis.IRedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author hua
 * @description 医疗服务实现类
 * @create 2025-04-02 22:51
 */
@Slf4j
@Service
public class MedicalServiceImpl implements IMedicalService {

    // 使用ConcurrentHashMap缓存用户在线状态，避免频繁查询Redis
    private static final Map<Long, Boolean> USER_ONLINE_STATUS = new ConcurrentHashMap<>();
    
    @Autowired
    private IDiagnosisService diagnosisService;
    
    @Autowired
    private IFeedbackMessageService feedbackMessageService;
    
    @Autowired
    private IPatientService patientService;
    
    @Autowired
    private IDoctorService doctorService;
    
    @Autowired
    private IUserService userService;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private IRedisService redisService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Override
    public List<DiagnosisVO> getPatientDiagnoses(Long patientId) {
        log.info("获取患者的诊断记录列表, patientId: {}", patientId);
        
        if (patientId == null) {
            throw new BusinessException("患者ID不能为空");
        }
        
        List<Diagnosis> diagnoses = diagnosisService.getDiagnosesByPatientId(patientId);
        if (diagnoses == null || diagnoses.isEmpty()) {
            return new ArrayList<>();
        }
        
        return convertToDiagnosisVOList(diagnoses);
    }

    @Override
    public List<DiagnosisVO> getDoctorDiagnoses(Long doctorId) {
        log.info("获取医生的诊断记录列表, doctorId: {}", doctorId);
        
        if (doctorId == null) {
            throw new BusinessException("医生ID不能为空");
        }
        
        List<Diagnosis> diagnoses = diagnosisService.getDiagnosesByDoctorId(doctorId);
        if (diagnoses == null || diagnoses.isEmpty()) {
            return new ArrayList<>();
        }
        
        return convertToDiagnosisVOList(diagnoses);
    }

    @Override
    public DiagnosisVO getDiagnosisDetail(Long diagId) {
        log.info("获取诊断详情, diagId: {}", diagId);
        
        if (diagId == null) {
            throw new BusinessException("诊断ID不能为空");
        }
        
        Diagnosis diagnosis = diagnosisService.getById(diagId);
        if (diagnosis == null) {
            throw new BusinessException("诊断记录不存在");
        }
        
        return convertToDiagnosisVO(diagnosis);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FeedbackMessageDTO sendFeedbackMessage(Long diagId, String content, Integer senderType, Long senderId) {
        log.info("发送诊后反馈消息, diagId: {}, senderType: {}, senderId: {}", diagId, senderType, senderId);
        
        // 参数验证
        if (diagId == null) {
            throw new BusinessException("诊断ID不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }
        if (senderType == null || (senderType != 0 && senderType != 1)) {
            throw new BusinessException("发送者类型无效");
        }
        if (senderId == null) {
            throw new BusinessException("发送者ID不能为空");
        }
        
        // 检查诊断记录是否存在
        Diagnosis diagnosis = diagnosisService.getById(diagId);
        if (diagnosis == null) {
            throw new BusinessException("诊断记录不存在");
        }
        
        // 检查发送者身份是否与诊断记录匹配
        if (senderType == 0 && !diagnosis.getPatientId().equals(senderId)) {
            throw new BusinessException("患者身份验证失败");
        } else if (senderType == 1 && !diagnosis.getDoctorId().equals(senderId)) {
            throw new BusinessException("医生身份验证失败");
        }
        
        // 如果是患者发送消息，检查是否在反馈期内(15天)
        if (senderType == 0 && !diagnosisService.isWithinFeedbackPeriod(diagId)) {
            throw new BusinessException("已超出反馈期限(15天)");
        }
        
        // 创建反馈消息记录
        FeedbackMessage message = new FeedbackMessage();
        message.setDiagId(diagId);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setReadStatus(0); // 默认未读
        message.setCreateTime(LocalDateTime.now());
        
        // 保存消息
        feedbackMessageService.save(message);
        
        // 获取接收者信息
        Long receiverId;
        String receiverName;
        if (senderType == 0) {
            // 患者发送，接收者是医生
            receiverId = diagnosis.getDoctorId();
            Doctor doctor = doctorService.getById(diagnosis.getDoctorId());
            receiverName = doctor != null ? doctor.getName() : "未知医生";
        } else {
            // 医生发送，接收者是患者
            receiverId = diagnosis.getPatientId();
            Patient patient = patientService.getById(diagnosis.getPatientId());
            receiverName = patient != null ? patient.getName() : "未知患者";
        }
        
        // 创建发送者姓名
        String senderName;
        if (senderType == 0) {
            // 患者
            Patient patient = patientService.getById(senderId);
            senderName = patient != null ? patient.getName() : "未知患者";
        } else {
            // 医生
            Doctor doctor = doctorService.getById(senderId);
            senderName = doctor != null ? doctor.getName() : "未知医生";
        }
        
        // 创建消息DTO
        FeedbackMessageDTO messageDTO = new FeedbackMessageDTO();
        BeanUtils.copyProperties(message, messageDTO);
        messageDTO.setSenderName(senderName);
        messageDTO.setReceiverId(receiverId);
        messageDTO.setReceiverName(receiverName);
        
        // 检查接收者是否在线并发送消息
        boolean isOnline = isUserOnline(receiverId);
        if (isOnline) {
            // 通过WebSocket发送消息
            String destination = "/queue/feedback/" + receiverId;
            sendToWebSocket(destination, messageDTO);
        } else {
            // 通过RabbitMQ异步发送消息
            String routingKey = "user." + receiverId;
            sendToRabbitMQAsync(Constants.MessageKey.FEEDBACK_MESSAGE_QUEUE, routingKey, messageDTO);
            
            // 异步更新未读消息数量
            updateUnreadMessageCountAsync(receiverId, 1);
        }
        
        return messageDTO;
    }

    @Override
    public List<FeedbackMessageDTO> getFeedbackMessages(Long diagId) {
        log.info("获取诊断相关的所有反馈消息, diagId: {}", diagId);
        
        if (diagId == null) {
            throw new BusinessException("诊断ID不能为空");
        }
        
        // 检查诊断记录是否存在
        Diagnosis diagnosis = diagnosisService.getById(diagId);
        if (diagnosis == null) {
            throw new BusinessException("诊断记录不存在");
        }
        
        // 获取消息列表
        List<FeedbackMessage> messages = feedbackMessageService.getMessagesByDiagId(diagId);
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取患者和医生信息
        Patient patient = patientService.getById(diagnosis.getPatientId());
        Doctor doctor = doctorService.getById(diagnosis.getDoctorId());
        String patientName = patient != null ? patient.getName() : "未知患者";
        String doctorName = doctor != null ? doctor.getName() : "未知医生";
        
        // 转换为DTO对象
        return messages.stream().map(message -> {
            FeedbackMessageDTO dto = new FeedbackMessageDTO();
            BeanUtils.copyProperties(message, dto);
            
            // 设置发送者和接收者信息
            if (message.getSenderType() == 0) {
                // 患者发送
                dto.setSenderName(patientName);
                dto.setReceiverId(diagnosis.getDoctorId());
                dto.setReceiverName(doctorName);
            } else {
                // 医生发送
                dto.setSenderName(doctorName);
                dto.setReceiverId(diagnosis.getPatientId());
                dto.setReceiverName(patientName);
            }
            
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean markMessageAsRead(Long messageId) {
        log.info("标记消息为已读, messageId: {}", messageId);
        
        if (messageId == null) {
            throw new BusinessException("消息ID不能为空");
        }
        
        return feedbackMessageService.markAsRead(messageId);
    }

    @Override
    public boolean markAllMessagesAsRead(Long diagId, Long userId, Integer role) {
        log.info("标记诊断相关的所有消息为已读, diagId: {}, userId: {}, role: {}", diagId, userId, role);
        
        if (diagId == null) {
            throw new BusinessException("诊断ID不能为空");
        }
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        if (role == null) {
            throw new BusinessException("角色不能为空");
        }
        
        return feedbackMessageService.markAllAsRead(diagId, userId, role);
    }

    @Override
    public int getUnreadMessageCount(Long userId, Integer role) {
        log.info("获取用户的未读消息数量, userId: {}, role: {}", userId, role);
        
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        if (role == null) {
            throw new BusinessException("角色不能为空");
        }
        
        return feedbackMessageService.getUnreadMessageCount(userId, role);
    }

    @Override
    public boolean canFeedback(Long diagId) {
        log.info("检查诊断是否可以进行反馈, diagId: {}", diagId);
        
        if (diagId == null) {
            throw new BusinessException("诊断ID不能为空");
        }
        
        return diagnosisService.isWithinFeedbackPeriod(diagId);
    }
    
    @Override
    public Long getPatientIdByUserId(Long userId) {
        log.info("根据用户ID获取患者ID, userId: {}", userId);
        
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        
        Patient patient = patientService.getByUserId(userId);
        if (patient == null) {
            throw new BusinessException("未找到患者信息");
        }
        
        return patient.getPatientId();
    }
    
    @Override
    public Long getDoctorIdByUserId(Long userId) {
        log.info("根据用户ID获取医生ID, userId: {}", userId);
        
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        
        Doctor doctor = doctorService.getDoctorByUserId(userId);
        if (doctor == null) {
            throw new BusinessException("未找到医生信息");
        }
        
        return doctor.getDoctorId();
    }
    
    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return 是否在线
     */
    private boolean isUserOnline(Long userId) {
        // 先从缓存中获取
        Boolean cached = USER_ONLINE_STATUS.get(userId);
        if (cached != null) {
            return cached;
        }
        
        // 缓存中不存在，通过Sa-Token检查
        boolean isOnline = StpUtil.isLogin(userId);
        
        // 更新缓存
        USER_ONLINE_STATUS.put(userId, isOnline);
        
        return isOnline;
    }
    
    /**
     * 同步发送WebSocket消息
     * @param destination 目标
     * @param message 消息
     */
    private void sendToWebSocket(String destination, Object message) {
        try {
            log.info("发送消息到WebSocket, destination: {}", destination);
            messagingTemplate.convertAndSend(destination, message);
            log.info("WebSocket消息发送成功");
        } catch (Exception e) {
            log.error("WebSocket消息发送异常", e);
        }
    }
    
    /**
     * 异步发送消息到RabbitMQ
     * @param exchange 交换机
     * @param routingKey 路由键
     * @param message 消息
     */
    @Override
    @Async("threadPoolTaskExecutor")
    public void sendToRabbitMQAsync(String exchange, String routingKey, Object message) {
        try {
            log.info("异步发送消息到RabbitMQ, exchange: {}, routingKey: {}", exchange, routingKey);
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.info("RabbitMQ消息发送成功");
        } catch (Exception e) {
            log.error("RabbitMQ消息发送异常", e);
        }
    }
    
    /**
     * 异步更新用户未读消息数量
     * @param userId 用户ID
     * @param count 增加的数量
     */
    @Override
    @Async("threadPoolTaskExecutor")
    public void updateUnreadMessageCountAsync(Long userId, int count) {
        try {
            log.info("异步更新用户未读消息数量, userId: {}, count: {}", userId, count);
            String redisKey = Constants.RedisKey.MESSAGE_USER + userId;
            if (redisService.isExists(redisKey)) {
                redisService.incrBy(redisKey, count);
            } else {
                redisService.setValue(redisKey, count, 30 * 24 * 60 * 60); // 30天过期
            }
            log.info("更新未读消息数量成功");
        } catch (Exception e) {
            log.error("更新未读消息数量异常", e);
        }
    }
    
    /**
     * 将诊断记录转换为VO对象
     * @param diagnosis 诊断记录
     * @return 诊断记录VO
     */
    private DiagnosisVO convertToDiagnosisVO(Diagnosis diagnosis) {
        if (diagnosis == null) {
            return null;
        }
        
        DiagnosisVO vo = new DiagnosisVO();
        BeanUtils.copyProperties(diagnosis, vo);
        
        // 设置是否可以反馈
        vo.setCanFeedback(diagnosisService.isWithinFeedbackPeriod(diagnosis.getDiagId()));
        
        // 获取患者信息
        Patient patient = patientService.getById(diagnosis.getPatientId());
        if (patient != null) {
            vo.setPatientName(patient.getName());
        }
        
        // 获取医生信息
        Doctor doctor = doctorService.getById(diagnosis.getDoctorId());
        if (doctor != null) {
            vo.setDoctorName(doctor.getName());
            vo.setDoctorTitle(doctor.getTitle());
        }
        
        return vo;
    }
    
    /**
     * 将诊断记录列表转换为VO对象列表
     * @param diagnoses 诊断记录列表
     * @return 诊断记录VO列表
     */
    private List<DiagnosisVO> convertToDiagnosisVOList(List<Diagnosis> diagnoses) {
        if (diagnoses == null || diagnoses.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取所有患者ID和医生ID
        List<Long> patientIds = diagnoses.stream()
                .map(Diagnosis::getPatientId)
                .distinct()
                .collect(Collectors.toList());
        
        List<Long> doctorIds = diagnoses.stream()
                .map(Diagnosis::getDoctorId)
                .distinct()
                .collect(Collectors.toList());
        
        // 批量查询患者和医生信息
        Map<Long, Patient> patientMap = patientService.listByIds(patientIds).stream()
                .collect(Collectors.toMap(Patient::getPatientId, patient -> patient));
        
        Map<Long, Doctor> doctorMap = doctorService.listByIds(doctorIds).stream()
                .collect(Collectors.toMap(Doctor::getDoctorId, doctor -> doctor));
        
        // 转换为VO对象
        return diagnoses.stream().map(diagnosis -> {
            DiagnosisVO vo = new DiagnosisVO();
            BeanUtils.copyProperties(diagnosis, vo);
            
            // 设置是否可以反馈
            vo.setCanFeedback(diagnosisService.isWithinFeedbackPeriod(diagnosis.getDiagId()));
            
            // 设置患者信息
            Patient patient = patientMap.get(diagnosis.getPatientId());
            if (patient != null) {
                vo.setPatientName(patient.getName());
            }
            
            // 设置医生信息
            Doctor doctor = doctorMap.get(diagnosis.getDoctorId());
            if (doctor != null) {
                vo.setDoctorName(doctor.getName());
                vo.setDoctorTitle(doctor.getTitle());
            }
            
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public User getCurrentUser() {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            return userService.getById(userId);
        } catch (Exception e) {
            log.error("获取当前用户异常", e);
            throw new BusinessException("获取当前用户失败");
        }
    }
    
    @Override
    public boolean isCurrentPatient(Long patientId) {
        try {
            User user = getCurrentUser();
            
            // 如果是患者角色，检查patientId是否对应
            if (user.getRole() == 0) {
                Long currentPatientId = getPatientIdByUserId(user.getId());
                return patientId.equals(currentPatientId);
            }
            
            return false;
        } catch (Exception e) {
            log.error("判断是否为当前患者异常", e);
            return false;
        }
    }
    
    @Override
    public boolean isCurrentDoctor(Long doctorId) {
        try {
            User user = getCurrentUser();
            
            // 如果是医生角色，检查doctorId是否对应
            if (user.getRole() == 1) {
                Long currentDoctorId = getDoctorIdByUserId(user.getId());
                return doctorId.equals(currentDoctorId);
            }
            
            return false;
        } catch (Exception e) {
            log.error("判断是否为当前医生异常", e);
            return false;
        }
    }
}
