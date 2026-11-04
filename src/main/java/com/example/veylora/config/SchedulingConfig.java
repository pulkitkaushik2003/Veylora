package com.example.veylora.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled jobs (the campaign dispatcher). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
