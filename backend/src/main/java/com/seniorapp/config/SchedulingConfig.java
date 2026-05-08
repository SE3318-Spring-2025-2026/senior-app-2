package com.seniorapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's @Scheduled task execution.
 * Required for GroupCleanupScheduler to run automatically.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
