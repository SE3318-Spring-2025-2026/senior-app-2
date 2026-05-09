package com.seniorapp.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProjectGithubIssueSyncScheduler {
    private final ProjectGithubIssueSyncService syncService;

    public ProjectGithubIssueSyncScheduler(ProjectGithubIssueSyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void runHourly() {
        syncService.syncEndedSprints();
    }
}
