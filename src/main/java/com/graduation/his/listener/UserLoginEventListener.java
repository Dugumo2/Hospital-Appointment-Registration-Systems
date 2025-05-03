package com.graduation.his.listener;

import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.SaLoginModel;
import com.graduation.his.common.Constants;
import com.graduation.his.domain.dto.FeedbackMessageDTO;
import com.graduation.his.domain.po.Doctor;
import com.graduation.his.domain.po.Patient;
import com.graduation.his.service.entity.IDoctorService;
import com.graduation.his.service.entity.IFeedbackMessageService;
import com.graduation.his.service.entity.IPatientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * 用户登录事件监听器
 * 用于在用户登录时推送积压的消息
 * @author hua
 */
@Slf4j
@Component
public class UserLoginEventListener implements SaTokenListener {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    
    @Autowired
    private IDoctorService doctorService;
    
    @Autowired
    private IPatientService patientService;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginModel loginModel) {
        // 登录成功事件
        try {
            // 用户ID
            Long userId = Long.valueOf(loginId.toString());
            log.info("用户登录成功，userId: {}", userId);
            
            // 推送积压消息的逻辑
            pushPendingMessages(userId);
        } catch (Exception e) {
            log.error("处理用户登录事件异常", e);
        }
    }
    
    /**
     * 推送用户的积压消息
     * @param userId 用户ID
     */
    private void pushPendingMessages(Long userId) {
        try {
            // 直接使用用户ID创建队列名
            String userQueueName = "user.queue." + userId;
            Properties queueProps = rabbitAdmin.getQueueProperties(userQueueName);
            if (queueProps == null) {
                log.info("用户队列不存在，没有待处理消息: {}", userQueueName);
                return;
            }
            
            // 获取队列中的消息数量
            Integer messageCount = (Integer) queueProps.get("QUEUE_MESSAGE_COUNT");
            log.info("用户队列 {} 中有 {} 条消息", userQueueName, messageCount);
            
            if (messageCount == null || messageCount == 0) {
                log.info("用户队列中没有消息，不需要推送");
                return;
            }
            
            // 批量获取消息
            List<FeedbackMessageDTO> messages = new ArrayList<>();
            int batchSize = Math.min(messageCount, 20); // 每批最多处理20条消息
            int receivedCount = 0;
            
            // 批量接收消息
            for (int i = 0; i < batchSize; i++) {
                Object message = rabbitTemplate.receiveAndConvert(userQueueName, 50); // 50ms超时
                if (message == null) {
                    break; // 没有更多消息
                }
                
                if (message instanceof FeedbackMessageDTO) {
                    messages.add((FeedbackMessageDTO) message);
                    receivedCount++;
                }
            }
            
            if (messages.isEmpty()) {
                log.info("没有获取到有效消息，不需要推送");
                return;
            }
            
            // 批量推送消息到WebSocket
            log.info("准备批量推送{}条消息给用户{}", messages.size(), userId);
            
            // 为了在Lambda中使用，需要使变量为final
            final int finalReceivedCount = receivedCount;
            final List<FeedbackMessageDTO> finalMessages = new ArrayList<>(messages);
            
            // 创建批处理线程，避免阻塞登录流程
            CompletableFuture.runAsync(() -> {
                try {
                    // 批量发送消息
                    for (FeedbackMessageDTO msg : finalMessages) {
                        // 只发送消息内容，不再发送未读计数
                        java.util.Map<String, Object> payload = new java.util.HashMap<>();
                        payload.put("type", Constants.WebSocketConstants.TYPE_MESSAGE);
                        
                        java.util.Map<String, Object> messageData = new java.util.HashMap<>();
                        messageData.put("id", msg.getMessageId());
                        messageData.put("chatId", msg.getDiagId());
                        messageData.put("content", msg.getContent());
                        messageData.put("sender", msg.getSenderId());
                        messageData.put("senderName", msg.getSenderName());
                        messageData.put("senderType", msg.getSenderType());
                        messageData.put("timestamp", msg.getCreateTime() != null ? 
                                msg.getCreateTime().toInstant(java.time.ZoneOffset.UTC).toEpochMilli() : System.currentTimeMillis());
                        
                        payload.put("message", messageData);
                        
                        messagingTemplate.convertAndSendToUser(
                            userId.toString(), 
                            Constants.WebSocketConstants.FEEDBACK_QUEUE, 
                            payload
                        );
                    }
                    
                    log.info("成功推送{}条消息给用户{}", finalMessages.size(), userId);
                    
                    // 如果还有更多消息，递归处理下一批
                    if (finalReceivedCount == batchSize && messageCount > batchSize) {
                        log.info("队列中还有更多消息，继续处理下一批");
                        pushNextBatch(userId, userQueueName);
                    }
                } catch (Exception e) {
                    log.error("批量推送消息异常", e);
                }
            });
        } catch (Exception e) {
            log.error("处理用户推送消息异常", e);
        }
    }
    
    /**
     * 处理下一批消息
     * @param userId 用户ID
     * @param userQueueName 用户队列名
     */
    private void pushNextBatch(Long userId, String userQueueName) {
        try {
            // 检查队列状态
            Properties queueProps = rabbitAdmin.getQueueProperties(userQueueName);
            if (queueProps == null) {
                return; // 队列不存在
            }
            
            Integer messageCount = (Integer) queueProps.get("QUEUE_MESSAGE_COUNT");
            if (messageCount == null || messageCount == 0) {
                return; // 没有更多消息
            }
            
            // 批量获取消息
            List<FeedbackMessageDTO> messages = new ArrayList<>();
            int batchSize = Math.min(messageCount, 20); // 每批最多处理20条消息
            int receivedCount = 0;
            
            // 批量接收消息
            for (int i = 0; i < batchSize; i++) {
                Object message = rabbitTemplate.receiveAndConvert(userQueueName, 50); // 50ms超时
                if (message == null) {
                    break; // 没有更多消息
                }
                
                if (message instanceof FeedbackMessageDTO) {
                    messages.add((FeedbackMessageDTO) message);
                    receivedCount++;
                }
            }
            
            if (messages.isEmpty()) {
                return; // 没有获取到有效消息
            }
            
            // 批量发送消息
            for (FeedbackMessageDTO msg : messages) {
                // 只发送消息内容，不再发送未读计数
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("type", Constants.WebSocketConstants.TYPE_MESSAGE);
                
                java.util.Map<String, Object> messageData = new java.util.HashMap<>();
                messageData.put("id", msg.getMessageId());
                messageData.put("chatId", msg.getDiagId());
                messageData.put("content", msg.getContent());
                messageData.put("sender", msg.getSenderId());
                messageData.put("senderName", msg.getSenderName());
                messageData.put("senderType", msg.getSenderType());
                messageData.put("timestamp", msg.getCreateTime() != null ? 
                        msg.getCreateTime().toInstant(java.time.ZoneOffset.UTC).toEpochMilli() : System.currentTimeMillis());
                
                payload.put("message", messageData);
                
                messagingTemplate.convertAndSendToUser(
                    userId.toString(), 
                    Constants.WebSocketConstants.FEEDBACK_QUEUE, 
                    payload
                );
            }
            
            log.info("成功推送下一批{}条消息给用户{}", messages.size(), userId);
            
            // 如果还有更多消息，递归处理下一批
            if (receivedCount == batchSize && messageCount > batchSize) {
                log.info("队列中还有更多消息，继续处理下一批");
                pushNextBatch(userId, userQueueName);
            }
        } catch (Exception e) {
            log.error("处理下一批消息异常", e);
        }
    }
    
    // 以下是SaTokenListener接口的其他方法，实现空方法
    
    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
        // 登出事件，不需要处理
    }
    
    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        // 被踢出事件，不需要处理
    }
    
    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        // 被顶下线事件，不需要处理
    }
    
    @Override
    public void doDisable(String loginType, Object loginId, String service, int level, long disableTime) {
        // 账号被封禁事件，不需要处理
    }
    
    @Override
    public void doUntieDisable(String loginType, Object loginId, String service) {
        // 账号被解封事件，不需要处理
    }
    
    @Override
    public void doCreateSession(String id) {
        // 会话创建事件，不需要处理
    }
    
    @Override
    public void doLogoutSession(String id) {
        // 会话注销事件，不需要处理
    }
    
    @Override
    public void doRenewTimeout(String tokenValue, Object loginId, long timeout) {
        // Token续期事件，不需要处理
    }
    
    @Override
    public void doOpenSafe(String loginType, String tokenValue, String service, long safeTime) {
        // 二级认证开启事件，不需要处理
    }
    
    @Override
    public void doCloseSafe(String loginType, String tokenValue, String service) {
        // 二级认证关闭事件，不需要处理
    }
} 