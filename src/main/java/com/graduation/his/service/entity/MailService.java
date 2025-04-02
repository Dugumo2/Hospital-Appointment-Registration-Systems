package com.graduation.his.service.entity;

import cn.hutool.core.util.RandomUtil;
import com.graduation.his.common.Constants;
import com.graduation.his.utils.redis.RedissonService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮件服务类
 * @author hua
 */
@Service
@Slf4j
public class MailService {
    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.nickname:系统邮件}")  // 昵称，默认值为"系统邮件"
    private String nickname;

    @Autowired
    private RedissonService redissonService;

    /**
     * 验证码过期时间（分钟）
     */
    private static final long CODE_EXPIRE_MINUTES = 5 * 60 * 1000; // 5分钟，转换为毫秒

    /**
     * 发送验证码到指定邮箱
     * @param to 接收者邮箱
     */
    public void sendVerificationCode(String to) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail, nickname);
            helper.setTo(to);
            helper.setSubject("【XX医院门诊系统】您的验证码");

            // 生成6位随机数字验证码
            String code = RandomUtil.randomNumbers(6);

            // 使用RedissonService存储验证码
            redissonService.setValue(Constants.RedisKey.HIS_MAIL_CODE + to, code, CODE_EXPIRE_MINUTES);

            helper.setText("您的验证码是：" + code + "，5分钟内有效", false);
            javaMailSender.send(message);
            
            log.info("验证码已发送至邮箱：{}", to);
        } catch (Exception e) {
            log.error("发送邮件失败", e);
            throw new RuntimeException("发送邮件失败");
        }
    }
}
