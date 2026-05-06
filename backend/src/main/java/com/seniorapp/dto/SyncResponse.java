package com.seniorapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResponse {

    @Builder.Default
    private String jobId = UUID.randomUUID().toString();

    @Builder.Default
    private String status = "accepted";

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private String message;
}
