package com.graduation.his.listener;

import com.graduation.his.common.Constants;
import com.graduation.his.domain.dto.FeedbackMessageDTO;
import com.graduation.his.domain.po.Doctor;
import com.graduation.his.domain.po.Patient;
import com.graduation.his.service.entity.IDoctorService;
import com.graduation.his.service.entity.IPatientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 反馈消息监听器
 * @author hua
 */
@Slf4j
@Component
public class FeedbackMessageListener {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private IDoctorService doctorService;
    
    @Autowired
    private IPatientService patientService;

    /**
     * 监听反馈消息队列
     * @param message 消息
     */
    @RabbitListener(queues = Constants.MessageKey.FEEDBACK_QUEUE_NAME)
    public void receiveFeedbackMessage(FeedbackMessageDTO message) {
        log.info("接收到反馈消息: {}", message);
        try {
            // 从实体ID获取用户ID
            Long receiverEntityId = message.getReceiverId();
            
            // 根据发送者类型和接收者实体ID获取用户ID
            Long receiverUserId = null;
            if (message.getSenderType() == 0) {
                // 如果发送者是患者(0)，接收者是医生
                Doctor doctor = doctorService.getById(receiverEntityId);
                if (doctor != null) {
                    receiverUserId = doctor.getUserId();
                }
            } else {
                // 如果发送者是医生(1)，接收者是患者
                Patient patient = patientService.getById(receiverEntityId);
                if (patient != null) {
                    receiverUserId = patient.getUserId();
                }
            }
            
            if (receiverUserId == null) {
                log.error("无法找到接收者用户ID，消息可能无法送达");
                return;
            }
            
            // 使用用户ID创建队列名
            String userQueueName = "user.queue." + receiverUserId;
            
            // 发送到用户专属队列
            rabbitTemplate.convertAndSend(userQueueName, message);
            log.info("消息已放入用户专属队列: {}", userQueueName);
            
        } catch (Exception e) {
            log.error("处理反馈消息异常", e);
        }
    }
} 