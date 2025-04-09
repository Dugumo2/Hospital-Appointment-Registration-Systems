package com.graduation.his.listener;

import com.graduation.his.domain.dto.FeedbackMessageDTO;
import com.graduation.his.service.entity.IFeedbackMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 反馈消息监听器
 * @author hua
 */
@Slf4j
@Component
public class FeedbackMessageListener {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private IFeedbackMessageService feedbackMessageService;

    /**
     * 监听反馈消息队列
     * @param message 消息
     */
    @RabbitListener(queues = "feedback.message.queue")
    public void receiveFeedbackMessage(FeedbackMessageDTO message) {
        log.info("接收到反馈消息: {}", message);
        try {
            // 如果消息未读，则标记为已读
            if (message.getReadStatus() != null && message.getReadStatus() == 0) {
                feedbackMessageService.markAsRead(message.getMessageId());
            }
            
            // 如果用户已上线，通过WebSocket推送消息
            String destination = "/queue/feedback/" + message.getReceiverId();
            messagingTemplate.convertAndSend(destination, message);
            log.info("消息推送成功");
        } catch (Exception e) {
            log.error("处理反馈消息异常", e);
        }
    }
} 