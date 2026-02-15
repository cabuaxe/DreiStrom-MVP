package de.dreistrom.calendar.config;

import de.dreistrom.calendar.service.DailyReminderJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz scheduler configuration for daily reminder checks.
 * Runs every day at 07:00 Europe/Berlin time.
 */
@Configuration
public class ReminderJobConfig {

    @Bean
    public JobDetail dailyReminderJobDetail() {
        return JobBuilder.newJob(DailyReminderJob.class)
                .withIdentity("dailyReminderJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger dailyReminderTrigger(JobDetail dailyReminderJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(dailyReminderJobDetail)
                .withIdentity("dailyReminderTrigger")
                .withSchedule(CronScheduleBuilder
                        .dailyAtHourAndMinute(7, 0)
                        .inTimeZone(java.util.TimeZone.getTimeZone("Europe/Berlin")))
                .build();
    }
}
