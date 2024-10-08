package org.example.jvcarsharingservice.service.notification;

import lombok.RequiredArgsConstructor;
import org.example.jvcarsharingservice.telegram.NotificationBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationBot notificationBot;
    @Value("${telegram.admin.chat.id}")
    private String adminChatId;

    public void notifyNewRentalsCreated(String message) {
        String info = """
                new rentals created: 
                """ + message;
        notificationBot.sendMessage(adminChatId, info);
    }

    public void notifyOverdueRentals(String message) {
        String info = """
                overdue rentals: 
                """ + message;
        notificationBot.sendMessage(adminChatId, info);
    }

    public void notifySuccessfulPayments(String message) {
        String info = """
                successful payments: 
                """ + message;
        notificationBot.sendMessage(adminChatId, info);
    }
}
