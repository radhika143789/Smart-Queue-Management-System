package com.smartqueue.notification.provider;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationProvider implements NotificationProvider {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.properties.mail.from}")
    private String fromEmail;

    @Override
    public boolean supports(String type) {
        return "EMAIL".equals(type);
    }

    @Override
    public void send(String recipient, String subject, String body) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(recipient);
            helper.setSubject(subject);
            
            // Sending as HTML. Using plain body string as simple HTML
            helper.setText(body, true);
            
            javaMailSender.send(message);
            log.info("Email sent successfully to {}", recipient);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", recipient, e.getMessage(), e);
            throw new RuntimeException("Email sending failed", e);
        }
    }
}
