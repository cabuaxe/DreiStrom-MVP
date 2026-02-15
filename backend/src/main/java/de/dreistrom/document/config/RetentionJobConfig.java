package de.dreistrom.document.config;

import de.dreistrom.document.service.RetentionExpiryJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class RetentionJobConfig {

    @Bean
    public JobDetail retentionExpiryJobDetail() {
        return JobBuilder.newJob(RetentionExpiryJob.class)
                .withIdentity("retentionExpiryJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger retentionExpiryTrigger(JobDetail retentionExpiryJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(retentionExpiryJobDetail)
                .withIdentity("retentionExpiryTrigger")
                .withSchedule(CronScheduleBuilder
                        .dailyAtHourAndMinute(2, 0)
                        .inTimeZone(TimeZone.getTimeZone("Europe/Berlin")))
                .build();
    }
}
