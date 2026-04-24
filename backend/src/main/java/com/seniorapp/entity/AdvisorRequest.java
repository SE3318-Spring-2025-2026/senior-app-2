package com.seniorapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "advisor_requests")
public class AdvisorRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private UserGroup group;

    @ManyToOne
    @JoinColumn(name = "professor_id", nullable = false)
    private User professor;

    @Enumerated(EnumType.STRING)
    private GroupInviteStatus status = GroupInviteStatus.PENDING;

    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}