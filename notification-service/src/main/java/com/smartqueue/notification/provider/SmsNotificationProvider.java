package com.smartqueue.notification.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsNotificationProvider implements NotificationProvider {

    @Value("${app.sms.mock:true}")
    private boolean mockMode;

    @Override
    public boolean supports(String type) { 
        return "SMS".equals(type); 
    }

    @Override
    public void send(String recipient, String subject, String body) {
        if (mockMode) {
            log.info("[MOCK SMS] To: {} | Message: {}", recipient, body);
            return;
        }
        // TODO: Replace with real Twilio SDK call:
        // Twilio.init(accountSid, authToken);
        // Message.creator(new PhoneNumber(recipient), new PhoneNumber(from), body).create();
        log.info("SMS sent to {}", recipient);
    }
}
