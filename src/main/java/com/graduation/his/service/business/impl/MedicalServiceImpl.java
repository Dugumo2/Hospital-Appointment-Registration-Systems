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
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import org.redisson.api.RMap;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.Properties;
import java.util.HashMap;
import java.time.ZoneOffset;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.his.domain.dto.DiagnosisDTO;
import com.graduation.his.domain.po.Appointment;
import com.graduation.his.service.entity.IAppointmentService;

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
    private RabbitAdmin rabbitAdmin;
    
    @Autowired
    private IRedisService redisService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private IAppointmentService appointmentService;
    
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
        Long receiverEntityId; // 接收者实体ID（医生ID或患者ID）
        Long receiverUserId; // 接收者用户ID
        String receiverName;
        if (senderType == 0) {
            // 患者发送，接收者是医生
            receiverEntityId = diagnosis.getDoctorId();
            Doctor doctor = doctorService.getById(diagnosis.getDoctorId());
            receiverName = doctor != null ? doctor.getName() : "未知医生";
            // 获取医生对应的用户ID
            receiverUserId = doctor != null ? doctor.getUserId() : null;
        } else {
            // 医生发送，接收者是患者
            receiverEntityId = diagnosis.getPatientId();
            Patient patient = patientService.getById(diagnosis.getPatientId());
            receiverName = patient != null ? patient.getName() : "未知患者";
            // 获取患者对应的用户ID
            receiverUserId = patient != null ? patient.getUserId() : null;
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
        messageDTO.setReceiverId(receiverEntityId);
        messageDTO.setReceiverName(receiverName);
        
        // 检查接收者是否在线并发送消息
        boolean isOnline = isUserOnline(receiverUserId);
        if (isOnline) {
            // 即使用户在线通过WebSocket直接收到消息，也应该更新Redis中的未读消息计数
            // 因为用户可能没有查看该消息
            updateUnreadMessageCountAsync(receiverEntityId, diagId, 1);
            
            // 通过WebSocket点对点发送消息
            // 1. 发送消息内容
            sendMessageToWebSocket(receiverUserId, messageDTO);
            
            // 2. 发送未读计数更新
            int receiverRole = (senderType == 0) ? 1 : 0; // 如果发送者是患者(0)，接收者是医生(1)，反之亦然
            sendUnreadCountersToWebSocket(receiverUserId, receiverEntityId, receiverRole);
        } else {
            // 确保用户专属队列存在
            ensureUserQueueExists(receiverEntityId);
            
            // 通过RabbitMQ异步发送消息，使用实体ID作为路由键
            String routingKey = Constants.MessageKey.USER_ROUTING_KEY_PREFIX + receiverEntityId;
            sendToRabbitMQAsync(Constants.MessageKey.FEEDBACK_MESSAGE_QUEUE, routingKey, messageDTO);
            
            // 异步更新未读消息数量 - 使用实体ID，保持与数据库记录一致
            updateUnreadMessageCountAsync(receiverEntityId, diagId, 1);
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
    public boolean markAllMessagesAsRead(Long diagId, Long entityId, Integer role) {
        log.info("标记诊断相关的所有消息为已读, diagId: {}, entityId: {}, role: {}", diagId, entityId, role);
        
        if (diagId == null) {
            throw new BusinessException("诊断ID不能为空");
        }
        if (entityId == null) {
            throw new BusinessException("实体ID不能为空");
        }
        if (role == null) {
            throw new BusinessException("角色不能为空");
        }
        
        // 1. 标记数据库中的消息为已读
        boolean result = feedbackMessageService.markAllAsRead(diagId, entityId, role);
        
        // 2. 清空Redis中的未读消息计数
        try {
            String redisKey = Constants.RedisKey.MESSAGE_USER + entityId;
            // 获取之前的未读消息数量
            int unreadCount = getUnreadMessageCount(entityId, diagId);
            
            if (unreadCount > 0) {
                // 删除Hash中的字段
                redisService.getMap(redisKey).remove(diagId.toString());
                
                // 获取用户对象
                Long userId = getEntityUserId(entityId, role);
                if (userId != null) {
                    // 发送未读计数更新
                    sendUnreadCountersToWebSocket(userId, entityId, role);
                }
            }
        } catch (Exception e) {
            log.error("清空Redis中的未读消息计数异常", e);
        }
        
        return result;
    }

    @Override
    public int getUnreadMessageCount(Long entityId, Long diagId) {
        log.info("获取用户的特定诊断未读消息数量, entityId: {}, diagId: {}", entityId, diagId);
        
        if (entityId == null || diagId == null) {
            log.warn("获取未读消息数量参数错误: entityId={}, diagId={}", entityId, diagId);
            return 0;
        }
        
        // 从Redis Hash中获取特定诊断的未读消息数量
        String redisKey = Constants.RedisKey.MESSAGE_USER + entityId;
        Object value = redisService.getFromMap(redisKey, diagId.toString());
        
        if (value == null) {
            return 0;
        }
        
        try {
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof String) {
                return Integer.parseInt((String) value);
            } else {
                log.warn("Redis中存储的未读消息数量类型异常: {}", value.getClass().getName());
                return 0;
            }
        } catch (Exception e) {
            log.error("解析Redis中的未读消息数量异常", e);
            return 0;
        }
    }
    
    @Override
    public Map<String, Integer> getAllUnreadMessageCounts(Long entityId, Integer role) {
        log.info("获取用户的所有诊断未读消息数量, entityId: {}, role: {}", entityId, role);
        
        if (entityId == null) {
            log.warn("获取所有未读消息数量参数错误: entityId={}, role={}", entityId, role);
            return new HashMap<>();
        }
        
        try {
            // 从Redis Hash中获取所有诊断的未读消息数量
            String redisKey = Constants.RedisKey.MESSAGE_USER + entityId;
            RMap<String, Integer> redisMap = redisService.getMap(redisKey);
            
            if (redisMap == null || redisMap.isEmpty()) {
                // 如果Redis中没有数据，则从数据库中查询
                Map<String, Integer> countsFromDb = feedbackMessageService.getUnreadMessageCountsByEntityId(entityId, role);
                if (!countsFromDb.isEmpty()) {
                    // 将数据库查询结果存入Redis
                    for (Map.Entry<String, Integer> entry : countsFromDb.entrySet()) {
                        redisMap.put(entry.getKey(), entry.getValue());
                    }
                    redisMap.expire(Duration.ofDays(30)); // 设置30天过期时间
                }
                return countsFromDb;
            }
            
            // 使用HashMap复制RedissonMap的内容，避免直接返回可能导致的并发问题
            Map<String, Integer> result = new HashMap<>(redisMap);
            return result;
        } catch (Exception e) {
            log.error("获取所有未读消息数量异常", e);
            return new HashMap<>();
        }
    }
    
    /**
     * 根据实体ID和角色获取用户ID
     * @param entityId 实体ID
     * @param role 用户角色
     * @return 用户ID
     */
    private Long getEntityUserId(Long entityId, Integer role) {
        try {
            if (role == 0) {
                // 患者
                Patient patient = patientService.getById(entityId);
                return patient != null ? patient.getUserId() : null;
            } else if (role == 1) {
                // 医生
                Doctor doctor = doctorService.getById(entityId);
                return doctor != null ? doctor.getUserId() : null;
            }
            return null;
        } catch (Exception e) {
            log.error("获取实体对应的用户ID异常", e);
            return null;
        }
    }
    
    /**
     * 发送消息到WebSocket
     * @param userId 用户ID
     * @param message 消息内容
     */
    private void sendMessageToWebSocket(Long userId, FeedbackMessageDTO message) {
        try {
            // 构造消息对象
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", Constants.WebSocketConstants.TYPE_MESSAGE);
            
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("id", message.getMessageId());
            messageData.put("chatId", message.getDiagId());
            messageData.put("content", message.getContent());
            messageData.put("sender", message.getSenderId());
            messageData.put("senderName", message.getSenderName());
            messageData.put("senderType", message.getSenderType());
            messageData.put("timestamp", message.getCreateTime() != null ? 
                    message.getCreateTime().toInstant(ZoneOffset.UTC).toEpochMilli() : System.currentTimeMillis());
            
            payload.put("message", messageData);
            
            // 发送WebSocket消息
            messagingTemplate.convertAndSendToUser(
                userId.toString(), 
                Constants.WebSocketConstants.FEEDBACK_QUEUE, 
                payload
            );
            log.info("发送消息到WebSocket成功: userId={}, messageId={}", userId, message.getMessageId());
        } catch (Exception e) {
            log.error("发送消息到WebSocket异常", e);
        }
    }
    
    /**
     * 发送未读计数更新到WebSocket
     * @param userId 用户ID
     * @param entityId 实体ID（患者ID或医生ID）
     * @param role 用户角色
     */
    private void sendUnreadCountersToWebSocket(Long userId, Long entityId, Integer role) {
        try {
            // 获取所有未读计数
            Map<String, Integer> unreadCounts = getAllUnreadMessageCounts(entityId, role);
            
            // 构造消息对象
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", Constants.WebSocketConstants.TYPE_UNREAD_COUNTER);
            payload.put("counters", unreadCounts);
            
            // 发送WebSocket消息
            messagingTemplate.convertAndSendToUser(
                userId.toString(), 
                Constants.WebSocketConstants.FEEDBACK_QUEUE, 
                payload
            );
            log.info("发送未读计数更新到WebSocket成功: userId={}", userId);
        } catch (Exception e) {
            log.error("发送未读计数更新到WebSocket异常", e);
        }
    }

    /**
     * 异步更新用户未读消息数量
     * @param entityId 实体ID
     * @param diagId 诊断ID
     * @param count 增加的数量
     */
    @Override
    @Async("threadPoolTaskExecutor")
    public void updateUnreadMessageCountAsync(Long entityId, Long diagId, int count) {
        try {
            log.info("异步更新用户未读消息数量, entityId: {}, diagId: {}, count: {}", entityId, diagId, count);
            
            String redisKey = Constants.RedisKey.MESSAGE_USER + entityId;
            RMap<String, Integer> redisMap = redisService.getMap(redisKey);
            
            // 获取当前未读数
            Integer currentCount = redisMap.get(diagId.toString());
            if (currentCount == null) {
                currentCount = 0;
            }
            
            // 更新未读数
            int newCount = currentCount + count;
            if (newCount <= 0) {
                // 移除键值对
                redisMap.remove(diagId.toString());
            } else {
                // 设置新的未读数
                redisMap.put(diagId.toString(), newCount);
            }
            
            // 设置过期时间
            redisMap.expire(Duration.ofDays(30)); // 30天过期
            
            log.info("更新未读消息数量成功: diagId={}, newCount={}", diagId, newCount);
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
    
    /**
     * 确保用户专属队列存在
     * @param entityId 实体ID（患者ID或医生ID）
     */
    private void ensureUserQueueExists(Long entityId) {
        try {
            String queueName = "user.queue." + entityId;
            
            // 检查队列是否存在
            Properties queueProps = rabbitAdmin.getQueueProperties(queueName);
            if (queueProps == null) {
                log.info("用户队列不存在，创建队列: {}", queueName);
                
                // 创建队列
                Queue queue = QueueBuilder.durable(queueName)
                        .withArgument("x-dead-letter-exchange", "dead.letter.exchange")
                        .withArgument("x-dead-letter-routing-key", "dead.letter.routing.key")
                        .withArgument("x-message-ttl", 30L * 24 * 60 * 60 * 1000) // 30天过期
                        .withArgument("x-queue-mode", "lazy") // 将队列设置为lazy模式
                        .build();
                rabbitAdmin.declareQueue(queue);
                
                // 创建绑定关系
                String routingKey = Constants.MessageKey.USER_ROUTING_KEY_PREFIX + entityId;
                Binding binding = BindingBuilder.bind(queue)
                        .to(new TopicExchange(Constants.MessageKey.FEEDBACK_MESSAGE_QUEUE))
                        .with(routingKey);
                rabbitAdmin.declareBinding(binding);
                
                log.info("为实体ID: {}创建了专用队列: {}", entityId, queueName);
            }
        } catch (Exception e) {
            log.error("创建用户队列异常", e);
        }
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

    @Override
    public DiagnosisVO createDiagnosis(DiagnosisDTO dto) {
        log.info("创建诊断记录, appointmentId: {}, doctorId: {}, patientId: {}", 
                dto.getAppointmentId(), dto.getDoctorId(), dto.getPatientId());
        
        // 参数验证
        if (dto.getAppointmentId() == null) {
            throw new BusinessException("预约ID不能为空");
        }
        if (dto.getDoctorId() == null) {
            throw new BusinessException("医生ID不能为空");
        }
        if (dto.getPatientId() == null) {
            throw new BusinessException("患者ID不能为空");
        }
        
        // 检查是否已存在该预约的诊断记录
        LambdaQueryWrapper<Diagnosis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Diagnosis::getAppointmentId, dto.getAppointmentId());
        Diagnosis existingDiagnosis = diagnosisService.getOne(queryWrapper);
        
        if (existingDiagnosis != null) {
            throw new BusinessException("该预约已存在诊断记录，诊断记录一旦创建不可修改");
        }
        
        // 校验预约是否存在且状态是否正确
        try {
            Appointment appointment = appointmentService.getById(dto.getAppointmentId());
            if (appointment == null) {
                throw new BusinessException("预约记录不存在");
            }
            if (!appointment.getDoctorId().equals(dto.getDoctorId())) {
                throw new BusinessException("无权为其他医生的预约创建诊断记录");
            }
            if (!appointment.getPatientId().equals(dto.getPatientId())) {
                throw new BusinessException("预约患者信息不匹配");
            }
            // 确保预约处于合适的状态
            if (appointment.getStatus() != 0) { // 假设0是待就诊状态
                throw new BusinessException("该预约状态不允许创建诊断记录");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("校验预约信息异常", e);
            throw new BusinessException("校验预约信息失败");
        }
        
        // 创建诊断记录
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setAppointmentId(dto.getAppointmentId());
        diagnosis.setDoctorId(dto.getDoctorId());
        diagnosis.setPatientId(dto.getPatientId());
        diagnosis.setDiagnosisResult(dto.getDiagnosisResult());
        diagnosis.setExamination(dto.getExamination());
        diagnosis.setPrescription(dto.getPrescription());
        diagnosis.setAdvice(dto.getAdvice());
        
        LocalDateTime now = LocalDateTime.now();
        diagnosis.setCreateTime(now);
        diagnosis.setUpdateTime(now);
        
        // 保存诊断记录
        boolean success = diagnosisService.save(diagnosis);
        if (!success) {
            throw new BusinessException("创建诊断记录失败");
        }
        
        log.info("诊断记录创建成功, diagId: {}", diagnosis.getDiagId());
        
        // 更新预约状态为已就诊
        updateAppointmentStatusToCompleted(dto.getAppointmentId());
        
        // 返回诊断记录VO
        return convertToDiagnosisVO(diagnosis);
    }

    @Override
    public DiagnosisVO getDiagnosisByAppointmentId(Long appointmentId) {
        log.info("根据预约ID获取诊断记录, appointmentId: {}", appointmentId);
        
        if (appointmentId == null) {
            throw new BusinessException("预约ID不能为空");
        }
        
        // 查询诊断记录
        LambdaQueryWrapper<Diagnosis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Diagnosis::getAppointmentId, appointmentId);
        Diagnosis diagnosis = diagnosisService.getOne(queryWrapper);
        
        if (diagnosis == null) {
            log.info("未找到预约ID: {}的诊断记录", appointmentId);
            return null;
        }
        
        return convertToDiagnosisVO(diagnosis);
    }

    /**
     * 更新预约状态为已完成
     * @param appointmentId 预约ID
     */
    private void updateAppointmentStatusToCompleted(Long appointmentId) {
        try {
            if (appointmentId == null) {
                log.warn("预约ID为空，无法更新状态");
                return;
            }
            
            // 获取预约记录
            Appointment appointment = appointmentService.getById(appointmentId);
            if (appointment == null) {
                log.warn("未找到预约记录: {}", appointmentId);
                return;
            }
            
            // 更新预约状态为已完成(1)
            appointment.setStatus(1);
            appointment.setUpdateTime(LocalDateTime.now());
            
            boolean result = appointmentService.updateById(appointment);
            if (result) {
                log.info("预约状态已更新为已完成, appointmentId: {}", appointmentId);
            } else {
                log.error("预约状态更新失败, appointmentId: {}", appointmentId);
                throw new BusinessException("更新预约状态失败");
            }
        } catch (Exception e) {
            log.error("更新预约状态异常", e);
            throw new BusinessException("更新预约状态失败: " + e.getMessage());
        }
    }
}
