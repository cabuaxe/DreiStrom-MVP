package de.dreistrom.calendar.service;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quartz job that runs daily to check deadlines and send reminders.
 */
@Component
@Slf4j
public class DailyReminderJob implements Job {

    @Autowired
    private ReminderService reminderService;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("Running daily reminder check...");
        try {
            reminderService.dailyCheck();
        } catch (Exception e) {
            log.error("Daily reminder check failed: {}", e.getMessage(), e);
        }
    }
}
