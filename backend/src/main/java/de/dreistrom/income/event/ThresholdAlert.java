package de.dreistrom.income.event;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
public class ThresholdAlert {

    private final ThresholdType type;
    private final BigDecimal ratio;
    private final BigDecimal gewerbeRevenue;
    private final Long userId;
    private final int year;
    private final Instant occurredAt;

    public ThresholdAlert(ThresholdType type, BigDecimal ratio,
                          BigDecimal gewerbeRevenue, Long userId, int year) {
        this.type = type;
        this.ratio = ratio;
        this.gewerbeRevenue = gewerbeRevenue;
        this.userId = userId;
        this.year = year;
        this.occurredAt = Instant.now();
    }
}
