package de.dreistrom.income.listener;

import de.dreistrom.income.dto.AbfaerbungStatusResponse;
import de.dreistrom.income.event.ThresholdAlert;
import de.dreistrom.income.event.ThresholdType;
import de.dreistrom.income.service.DashboardService;
import de.dreistrom.income.service.IncomeSseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SseThresholdListener {

    private final IncomeSseEmitterService sseEmitterService;
    private final DashboardService dashboardService;

    @EventListener
    public void onThresholdAlert(ThresholdAlert alert) {
        if (alert.getType() != ThresholdType.ABFAERBUNG) {
            return;
        }
        AbfaerbungStatusResponse status = dashboardService.getAbfaerbungStatus(
                alert.getUserId(), alert.getYear());
        sseEmitterService.send(alert.getUserId(), status);
    }
}
