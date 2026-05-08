package com.seniorapp.service;

import com.seniorapp.entity.Notification;
import com.seniorapp.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void sendNotification_Success() {
        notificationService.sendNotification(1L, "Title", "Message", "/link");
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void markAsRead_Success() {
        Notification n = new Notification();
        n.setId(1L);
        n.setRecipientUserId(100L);
        n.setRead(false);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        notificationService.markAsRead(1L, 100L);

        assertThat(n.isRead()).isTrue();
        verify(notificationRepository).save(n);
    }

    @Test
    void markAsRead_Forbidden() {
        Notification n = new Notification();
        n.setId(1L);
        n.setRecipientUserId(200L);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        assertThrows(ResponseStatusException.class, () -> notificationService.markAsRead(1L, 100L));
    }
}
