package com.graduation.his.service.entity;

import cn.hutool.core.util.RandomUtil;
import com.graduation.his.common.Constants;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

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
    private StringRedisTemplate stringRedisTemplate;


    public void sendVerificationCode(String to) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail, nickname);
            helper.setTo(to);
            helper.setSubject("【XX医院门诊系统】您的验证码");

            String code = RandomUtil.randomNumbers(6);

            stringRedisTemplate.opsForValue().set(Constants.RedisKey.HIS_MAIL_CODE + to,code, 5, TimeUnit.MINUTES);

            helper.setText("您的验证码是：" + code + "，5分钟内有效", false);
            javaMailSender.send(message);

        } catch (Exception e) {
            log.error("发送邮件失败", e);
            throw new RuntimeException("发送邮件失败");
        }
    }
}
