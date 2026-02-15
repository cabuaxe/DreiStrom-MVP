package de.dreistrom.document.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

/**
 * Nightly Quartz job to process document retention expiry.
 * Unlocks documents past retention period, flags approaching expiry.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RetentionExpiryJob implements Job {

    private final DocumentVaultService documentVaultService;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("Running nightly document retention expiry check");
        documentVaultService.processRetentionExpiry();
        log.info("Document retention expiry check completed");
    }
}
