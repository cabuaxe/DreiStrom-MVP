package de.dreistrom.income.listener;

import de.dreistrom.income.event.ThresholdAlert;
import de.dreistrom.income.event.ThresholdType;
import de.dreistrom.income.service.ArbZGService;
import de.dreistrom.income.service.DashboardService;
import de.dreistrom.income.service.GewerbesteuerThresholdService;
import de.dreistrom.income.service.IncomeSseEmitterService;
import de.dreistrom.income.service.MandatoryFilingService;
import de.dreistrom.vat.service.KleinunternehmerStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SseThresholdListener {

    private final IncomeSseEmitterService sseEmitterService;
    private final DashboardService dashboardService;
    private final KleinunternehmerStatusService kleinunternehmerStatusService;
    private final GewerbesteuerThresholdService gewerbesteuerThresholdService;
    private final MandatoryFilingService mandatoryFilingService;
    private final ArbZGService arbZGService;

    @EventListener
    public void onThresholdAlert(ThresholdAlert alert) {
        Long userId = alert.getUserId();
        int year = alert.getYear();

        switch (alert.getType()) {
            case ABFAERBUNG -> sseEmitterService.send(userId, "abfaerbung",
                    dashboardService.getAbfaerbungStatus(userId, year));

            case KLEINUNTERNEHMER_CURRENT_YEAR, KLEINUNTERNEHMER_PROJECTED ->
                    sseEmitterService.send(userId, "kleinunternehmer",
                            kleinunternehmerStatusService.getStatus(userId, year));

            case GEWERBESTEUER_FREIBETRAG, BILANZIERUNG ->
                    sseEmitterService.send(userId, "gewerbesteuer",
                            gewerbesteuerThresholdService.getStatus(userId, year));

            case MANDATORY_FILING -> sseEmitterService.send(userId, "mandatory-filing",
                    mandatoryFilingService.getStatus(userId, year));

            case SOCIAL_INSURANCE, ARBZG_HOURS ->
                    sseEmitterService.send(userId, "social-insurance",
                            arbZGService.getStatus(userId, year));
        }
    }
}
