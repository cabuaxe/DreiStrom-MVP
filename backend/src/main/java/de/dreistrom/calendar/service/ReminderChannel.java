package de.dreistrom.calendar.service;

import de.dreistrom.calendar.domain.ComplianceEvent;
import de.dreistrom.calendar.domain.NotificationChannel;
import de.dreistrom.common.domain.AppUser;

/**
 * Strategy interface for delivering reminders through different channels.
 */
public interface ReminderChannel {

    NotificationChannel channel();

    void send(AppUser user, ComplianceEvent event, int daysBefore);
}
