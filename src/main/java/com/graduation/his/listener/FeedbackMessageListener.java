package com.graduation.his.listener;

import com.graduation.his.common.Constants;
import com.graduation.his.domain.dto.FeedbackMessageDTO;
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

    /**
     * 监听反馈消息队列
     * @param message 消息
     */
    @RabbitListener(queues = Constants.MessageKey.FEEDBACK_QUEUE_NAME)
    public void receiveFeedbackMessage(FeedbackMessageDTO message) {
        log.info("接收到反馈消息: {}", message);
        try {
            // 接收者ID是实体ID（患者ID或医生ID）
            Long receiverEntityId = message.getReceiverId();
            
            // 直接发送到用户专属队列
            String userQueueName = "user.queue." + receiverEntityId;
            
            // 发送到用户专属队列
            rabbitTemplate.convertAndSend(userQueueName, message);
            log.info("消息已放入用户专属队列: {}", userQueueName);
            
        } catch (Exception e) {
            log.error("处理反馈消息异常", e);
        }
    }
} 