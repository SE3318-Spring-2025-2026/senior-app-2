package com.seniorapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncRequest {

    @NotNull(message = "Source is required")
    private SyncSource source;

    @NotBlank(message = "Group ID is required")
    private String groupId;

    private IntegrationTokens integrationTokens;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntegrationTokens {
        private String pat;
        private String oauth;
    }

    public enum SyncSource {
        GITHUB,
        JIRA,
        BOTH
    }
}
