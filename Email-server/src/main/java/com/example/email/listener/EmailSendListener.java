package com.example.email.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 邮件发送监听器
 * 消费 EMAIL_SEND 主题消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "EMAIL_SEND",
        consumerGroup = "email_send_consumer_group"
)
public class EmailSendListener implements RocketMQListener<String> {

    private final EmailService emailService;

    @Override
    public void onMessage(String message) {
        try {
            log.info("收到邮件发送请求: {}", message);

            JSONObject emailData = JSON.parseObject(message);
            String type = emailData.getString("type");
            String to = emailData.getString("to");

            switch (type) {
                case "verification_code":
                    // 验证码邮件
                    String code = emailData.getString("code");
                    emailService.sendVerificationCode(to, code);
                    break;

                case "welcome":
                    // 欢迎邮件
                    String username = emailData.getString("username");
                    emailService.sendWelcomeEmail(to, username);
                    break;

                case "password_reset":
                    // 密码重置邮件
                    String resetToken = emailData.getString("resetToken");
                    emailService.sendPasswordResetEmail(to, resetToken);
                    break;

                case "login_alert":
                    // 登录异常通知
                    String ip = emailData.getString("ip");
                    String location = emailData.getString("location");
                    String time = emailData.getString("time");
                    emailService.sendLoginAlertEmail(to, ip, location, time);
                    break;

                case "custom":
                    // 自定义邮件
                    String subject = emailData.getString("subject");
                    String content = emailData.getString("content");
                    emailService.sendHtmlEmail(to, subject, content);
                    break;

                default:
                    log.warn("未知的邮件类型: {}", type);
            }

            log.info("邮件发送完成: type={}, to={}", type, to);

        } catch (Exception e) {
            log.error("邮件发送失败", e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }
}
