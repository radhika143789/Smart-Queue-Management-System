package com.smartqueue.notification.provider;

public interface NotificationProvider {
    void send(String recipient, String subject, String body);
    boolean supports(String type);
}
