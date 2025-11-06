package com.example.email.listener;

import com.example.common.dto.ConfirmationEmailTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Slf4j
@Service
public class UserListener {

    private final JavaMailSender javaMailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${server.user-service-url:http://localhost:9000}")
    private String userServiceUrl;
    
    @Value("${server.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public UserListener(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    /**
     * 监听邮件确认队列
     * 接收用户注册消息并发送确认邮件
     */
    @RabbitListener(queues = "email.confirmation.queue")
    public void handleConfirmationEmail(ConfirmationEmailTask task) {
        try {
            log.info("收到邮件确认任务: email={}, token={}", task.getEmail(), task.getToken());
            
            // Spring Boot 自动将 JSON 反序列化为 task 对象
            String email = task.getEmail();
            String token = task.getToken();

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(email);
            helper.setFrom(fromEmail);
            helper.setSubject("【协作平台】邮箱验证 - 请激活您的账户");
            
            String htmlContent = getHtmlContent(task);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);
            
            log.info("邮件发送成功: email={}", email);
            
        } catch (MessagingException e) {
            log.error("发送确认邮件失败: email={}, error={}", task.getEmail(), e.getMessage(), e);
            // 可以考虑重新抛出异常，让 RabbitMQ 重试
            throw new RuntimeException("发送邮件失败", e);
        }
    }

    /**
     * 生成邮件 HTML 内容
     */
    private String getHtmlContent(ConfirmationEmailTask task) {
        // 链接指向前端验证页面，前端再调用后端 API
        String confirmationLink = frontendUrl + "/verify-email?token=" + task.getToken();
        
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "<style>"
                + "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }"
                + ".container { max-width: 600px; margin: 0 auto; padding: 20px; }"
                + ".header { background-color: #1a73e8; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }"
                + ".content { background-color: #f9f9f9; padding: 30px; border: 1px solid #ddd; }"
                + ".button { display: inline-block; padding: 12px 30px; background-color: #1a73e8; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }"
                + ".footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'>"
                + "<h1>欢迎注册协作平台</h1>"
                + "</div>"
                + "<div class='content'>"
                + "<h2>您好！</h2>"
                + "<p>感谢您注册我们的协作平台。为了确保您的账户安全，请点击下方按钮验证您的邮箱地址：</p>"
                + "<div style='text-align: center;'>"
                + "<a href='" + confirmationLink + "' class='button'>验证邮箱</a>"
                + "</div>"
                + "<p>或者复制以下链接到浏览器打开：</p>"
                + "<p style='word-break: break-all; background-color: #fff; padding: 10px; border: 1px solid #ddd;'>"
                + confirmationLink
                + "</p>"
                + "<p><strong>注意：</strong>此验证链接将在 <strong>12 小时</strong>后失效。</p>"
                + "</div>"
                + "<div class='footer'>"
                + "<p>此邮件由系统自动发送，请勿直接回复。</p>"
                + "<p>如有疑问，请联系我们的客服团队。</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }
}
