package com.example.veylora.Service;

import com.example.veylora.Model.DeviceTask;

/**
 * Published when a device task reaches its final state, so campaigns can react
 * without DeviceTaskService knowing about campaigns.
 * retryable is false when the result is unclear (the phone never reported): retrying could send twice.
 */
public record TaskFinishedEvent(
        Long taskId,
        Long campaignRecipientId,
        DeviceTask.TaskType type,
        boolean success,
        String callOutcome,
        boolean retryable,
        String error) {}
