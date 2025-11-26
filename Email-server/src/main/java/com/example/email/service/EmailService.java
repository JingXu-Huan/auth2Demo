package com.example.email.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * 邮件服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${server.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * 发送验证码邮件
     */
    public void sendVerificationCode(String to, String code) {
        String subject = "【协作平台】邮箱验证码";
        String content = buildVerificationCodeTemplate(code);
        sendHtmlEmail(to, subject, content);
    }

    /**
     * 发送欢迎邮件
     */
    public void sendWelcomeEmail(String to, String username) {
        String subject = "【协作平台】欢迎注册";
        String content = buildWelcomeTemplate(username);
        sendHtmlEmail(to, subject, content);
    }

    /**
     * 发送密码重置邮件
     */
    public void sendPasswordResetEmail(String to, String resetToken) {
        String subject = "【协作平台】密码重置";
        String content = buildPasswordResetTemplate(resetToken);
        sendHtmlEmail(to, subject, content);
    }

    /**
     * 发送登录异常通知
     */
    public void sendLoginAlertEmail(String to, String ip, String location, String time) {
        String subject = "【协作平台】账户安全提醒";
        String content = buildLoginAlertTemplate(ip, location, time);
        sendHtmlEmail(to, subject, content);
    }

    /**
     * 发送HTML邮件
     */
    public void sendHtmlEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("邮件发送成功: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("邮件发送失败: to={}, subject={}", to, subject, e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    /**
     * 构建验证码邮件模板
     */
    private String buildVerificationCodeTemplate(String code) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"max-width: 600px; margin: 0 auto; padding: 20px; font-family: Arial, sans-serif;\">");
        sb.append("<h2 style=\"color: #333; border-bottom: 2px solid #007bff; padding-bottom: 10px;\">邮箱验证</h2>");
        sb.append("<p style=\"color: #666; font-size: 16px;\">您好！</p>");
        sb.append("<p style=\"color: #666; font-size: 16px;\">您的验证码是：</p>");
        sb.append("<div style=\"background: #f5f5f5; padding: 20px; text-align: center; margin: 20px 0;\">");
        sb.append("<span style=\"font-size: 32px; font-weight: bold; color: #007bff; letter-spacing: 5px;\">").append(code).append("</span>");
        sb.append("</div>");
        sb.append("<p style=\"color: #999; font-size: 14px;\">验证码有效期为 5 分钟，请勿泄露给他人。</p>");
        sb.append("<p style=\"color: #999; font-size: 14px;\">如果您没有请求此验证码，请忽略此邮件。</p>");
        sb.append("<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\">");
        sb.append("<p style=\"color: #999; font-size: 12px;\">此邮件由系统自动发送，请勿回复。</p>");
        sb.append("</div>");
        return sb.toString();
    }

    /**
     * 构建欢迎邮件模板
     */
    private String buildWelcomeTemplate(String username) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"max-width: 600px; margin: 0 auto; padding: 20px; font-family: Arial, sans-serif;\">");
        sb.append("<h2 style=\"color: #333; border-bottom: 2px solid #28a745; padding-bottom: 10px;\">欢迎加入</h2>");
        sb.append("<p style=\"color: #666; font-size: 16px;\">亲爱的 <strong>").append(username).append("</strong>，</p>");
        sb.append("<p style=\"color: #666; font-size: 16px;\">欢迎加入企业协作平台！我们很高兴您成为我们社区的一员。</p>");
        sb.append("<div style=\"background: #667eea; padding: 30px; text-align: center; margin: 20px 0; border-radius: 10px;\">");
        sb.append("<a href=\"").append(frontendUrl).append("/login\" style=\"color: white; text-decoration: none; font-size: 18px;\">开始使用</a>");
        sb.append("</div>");
        sb.append("<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\">");
        sb.append("<p style=\"color: #999; font-size: 12px;\">此邮件由系统自动发送，请勿回复。</p>");
        sb.append("</div>");
        return sb.toString();
    }

    /**
     * 构建密码重置邮件模板
     */
    private String buildPasswordResetTemplate(String resetToken) {
        String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"max-width: 600px; margin: 0 auto; padding: 20px; font-family: Arial, sans-serif;\">");
        sb.append("<h2 style=\"color: #333; border-bottom: 2px solid #dc3545; padding-bottom: 10px;\">密码重置</h2>");
        sb.append("<p style=\"color: #666; font-size: 16px;\">您好！</p>");
        sb.append("<p style=\"color: #666; font-size: 16px;\">我们收到了重置您密码的请求。请点击下面的按钮重置密码：</p>");
        sb.append("<div style=\"text-align: center; margin: 30px 0;\">");
        sb.append("<a href=\"").append(resetUrl).append("\" style=\"background: #dc3545; color: white; padding: 15px 30px; text-decoration: none; border-radius: 5px; font-size: 16px;\">重置密码</a>");
        sb.append("</div>");
        sb.append("<p style=\"color: #999; font-size: 14px;\">此链接有效期为 30 分钟。</p>");
        sb.append("<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\">");
        sb.append("<p style=\"color: #999; font-size: 12px;\">此邮件由系统自动发送，请勿回复。</p>");
        sb.append("</div>");
        return sb.toString();
    }

    /**
     * 构建登录异常通知模板
     */
    private String buildLoginAlertTemplate(String ip, String location, String time) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"max-width: 600px; margin: 0 auto; padding: 20px; font-family: Arial, sans-serif;\">");
        sb.append("<h2 style=\"color: #333; border-bottom: 2px solid #ffc107; padding-bottom: 10px;\">安全提醒</h2>");
        sb.append("<p style=\"color: #666; font-size: 16px;\">您好！</p>");
        sb.append("<p style=\"color: #666; font-size: 16px;\">我们检测到您的账户在新设备或新位置登录：</p>");
        sb.append("<div style=\"background: #fff3cd; padding: 20px; margin: 20px 0; border-radius: 5px; border-left: 4px solid #ffc107;\">");
        sb.append("<p style=\"margin: 5px 0;\"><strong>登录时间：</strong>").append(time).append("</p>");
        sb.append("<p style=\"margin: 5px 0;\"><strong>登录IP：</strong>").append(ip).append("</p>");
        sb.append("<p style=\"margin: 5px 0;\"><strong>登录地点：</strong>").append(location).append("</p>");
        sb.append("</div>");
        sb.append("<p style=\"color: #666; font-size: 14px;\">如果这是您本人的操作，请忽略此邮件。</p>");
        sb.append("<p style=\"color: #dc3545; font-size: 14px;\"><strong>如果这不是您的操作，建议您立即修改密码！</strong></p>");
        sb.append("<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\">");
        sb.append("<p style=\"color: #999; font-size: 12px;\">此邮件由系统自动发送，请勿回复。</p>");
        sb.append("</div>");
        return sb.toString();
    }
}
