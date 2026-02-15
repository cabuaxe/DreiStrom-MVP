package de.dreistrom.calendar.service;

import de.dreistrom.calendar.domain.ComplianceEvent;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Exports compliance events as iCalendar (.ics) format using iCal4j.
 */
@Service
public class ICalExportService {

    private static final String PRODUCT_ID = "-//DreiStrom//Compliance Calendar//DE";

    /**
     * Export a list of compliance events to iCal format.
     */
    public Calendar export(List<ComplianceEvent> events) {
        Calendar calendar = new Calendar();
        calendar.add(new ProdId(PRODUCT_ID));
        calendar.add(new CalScale("GREGORIAN"));
        calendar.add(new Version("2.0", "2.0"));
        calendar.add(new XProperty("X-WR-CALNAME", "DreiStrom Steuerkalender"));
        calendar.add(new XProperty("X-WR-TIMEZONE", "Europe/Berlin"));

        for (ComplianceEvent event : events) {
            calendar.add(toVEvent(event));
        }

        return calendar;
    }

    /**
     * Serialize calendar to iCal string.
     */
    public String exportAsString(List<ComplianceEvent> events) {
        return export(events).toString();
    }

    private VEvent toVEvent(ComplianceEvent event) {
        VEvent vEvent = new VEvent(event.getDueDate(), event.getTitle());

        vEvent.add(new Uid("dreistrom-compliance-" + event.getId() + "@dreistrom.de"));

        if (event.getDescription() != null) {
            vEvent.add(new Description(event.getDescription()));
        }

        // Map status
        switch (event.getStatus()) {
            case COMPLETED -> vEvent.add(new Status("CONFIRMED"));
            case CANCELLED -> vEvent.add(new Status("CANCELLED"));
            default -> vEvent.add(new Status("TENTATIVE"));
        }

        // Add categories based on event type
        String category = switch (event.getEventType()) {
            case UST_VA -> "USt-VA";
            case EST_VORAUSZAHLUNG -> "ESt-Vorauszahlung";
            case GEWST_VORAUSZAHLUNG -> "GewSt-Vorauszahlung";
            case EUER_FILING -> "EÜR";
            case EST_DECLARATION -> "Einkommensteuer";
            case GEWST_DECLARATION -> "Gewerbesteuer";
            case UST_DECLARATION -> "Umsatzsteuer";
            case ZM_REPORT -> "ZM-Meldung";
            case SOCIAL_INSURANCE -> "Sozialversicherung";
            case CUSTOM -> "Sonstiges";
        };
        vEvent.add(new Categories(category));

        // Add alarm reminders: 7 days and 1 day before
        VAlarm alarm7d = new VAlarm(Duration.ofDays(-7));
        alarm7d.add(new Action("DISPLAY"));
        alarm7d.add(new Description("Frist in 7 Tagen: " + event.getTitle()));
        vEvent.add(alarm7d);

        VAlarm alarm1d = new VAlarm(Duration.ofDays(-1));
        alarm1d.add(new Action("DISPLAY"));
        alarm1d.add(new Description("Frist morgen: " + event.getTitle()));
        vEvent.add(alarm1d);

        return vEvent;
    }
}
