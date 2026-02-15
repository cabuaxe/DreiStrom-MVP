package de.dreistrom.socialinsurance.service;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.socialinsurance.domain.SocialInsuranceEntry;
import de.dreistrom.socialinsurance.dto.SocialInsuranceStatus;
import de.dreistrom.socialinsurance.dto.SocialInsuranceStatus.RiskLevel;
import de.dreistrom.socialinsurance.repository.SocialInsuranceEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialInsuranceMonitorServiceTest {

    @Mock
    private SocialInsuranceEntryRepository entryRepository;

    @InjectMocks
    private SocialInsuranceMonitorService service;

    private final AppUser user = new AppUser("test@dreistrom.de", "hash", "Test User");

    @Test
    void returnsNoDataStatusWhenEmpty() {
        when(entryRepository.findByUserIdAndYearOrderByMonthAsc(any(), eq((short) 2026)))
                .thenReturn(Collections.emptyList());

        SocialInsuranceStatus status = service.getStatus(1L, 2026);

        assertThat(status.riskLevel()).isEqualTo(RiskLevel.SAFE);
        assertThat(status.riskMessage()).contains("Keine Daten");
        assertThat(status.monthlyData()).isEmpty();
    }

    @Test
    void assessesSafeStatusWhenEmploymentIsPrimary() {
        List<SocialInsuranceEntry> entries = List.of(
                new SocialInsuranceEntry(user, 2026, 1,
                        new BigDecimal("38.0"), new BigDecimal("10.0"),
                        new BigDecimal("4500"), new BigDecimal("2000")),
                new SocialInsuranceEntry(user, 2026, 2,
                        new BigDecimal("40.0"), new BigDecimal("8.0"),
                        new BigDecimal("4500"), new BigDecimal("1800"))
        );
        when(entryRepository.findByUserIdAndYearOrderByMonthAsc(any(), eq((short) 2026)))
                .thenReturn(entries);

        SocialInsuranceStatus status = service.getStatus(1L, 2026);

        assertThat(status.riskLevel()).isEqualTo(RiskLevel.SAFE);
        assertThat(status.hoursRiskFlag()).isFalse();
        assertThat(status.incomeRiskFlag()).isFalse();
        assertThat(status.avgSelfEmployedHoursWeekly()).isEqualByComparingTo("9.0");
        assertThat(status.totalEmploymentIncome()).isEqualByComparingTo("9000");
        assertThat(status.totalSelfEmployedIncome()).isEqualByComparingTo("3800");
    }

    @Test
    void detectsHoursWarningWhenSelfEmployedExceeds20h() {
        List<SocialInsuranceEntry> entries = List.of(
                new SocialInsuranceEntry(user, 2026, 1,
                        new BigDecimal("35.0"), new BigDecimal("22.0"),
                        new BigDecimal("4000"), new BigDecimal("2000")),
                new SocialInsuranceEntry(user, 2026, 2,
                        new BigDecimal("35.0"), new BigDecimal("24.0"),
                        new BigDecimal("4000"), new BigDecimal("2000"))
        );
        when(entryRepository.findByUserIdAndYearOrderByMonthAsc(any(), eq((short) 2026)))
                .thenReturn(entries);

        SocialInsuranceStatus status = service.getStatus(1L, 2026);

        assertThat(status.riskLevel()).isEqualTo(RiskLevel.WARNING);
        assertThat(status.hoursRiskFlag()).isTrue();
        assertThat(status.incomeRiskFlag()).isFalse();
        assertThat(status.riskMessage()).contains("20-Stunden-Grenze");
    }

    @Test
    void detectsIncomeWarningWhenSelfEmployedExceedsEmployment() {
        List<SocialInsuranceEntry> entries = List.of(
                new SocialInsuranceEntry(user, 2026, 1,
                        new BigDecimal("38.0"), new BigDecimal("15.0"),
                        new BigDecimal("3000"), new BigDecimal("5000")),
                new SocialInsuranceEntry(user, 2026, 2,
                        new BigDecimal("38.0"), new BigDecimal("15.0"),
                        new BigDecimal("3000"), new BigDecimal("5000"))
        );
        when(entryRepository.findByUserIdAndYearOrderByMonthAsc(any(), eq((short) 2026)))
                .thenReturn(entries);

        SocialInsuranceStatus status = service.getStatus(1L, 2026);

        assertThat(status.riskLevel()).isEqualTo(RiskLevel.WARNING);
        assertThat(status.hoursRiskFlag()).isFalse();
        assertThat(status.incomeRiskFlag()).isTrue();
        assertThat(status.riskMessage()).contains("Einkommen");
    }

    @Test
    void detectsCriticalWhenBothThresholdsExceeded() {
        List<SocialInsuranceEntry> entries = List.of(
                new SocialInsuranceEntry(user, 2026, 1,
                        new BigDecimal("30.0"), new BigDecimal("25.0"),
                        new BigDecimal("3000"), new BigDecimal("6000")),
                new SocialInsuranceEntry(user, 2026, 2,
                        new BigDecimal("30.0"), new BigDecimal("28.0"),
                        new BigDecimal("3000"), new BigDecimal("6500"))
        );
        when(entryRepository.findByUserIdAndYearOrderByMonthAsc(any(), eq((short) 2026)))
                .thenReturn(entries);

        SocialInsuranceStatus status = service.getStatus(1L, 2026);

        assertThat(status.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(status.hoursRiskFlag()).isTrue();
        assertThat(status.incomeRiskFlag()).isTrue();
        assertThat(status.riskMessage()).contains("Umklassifizierungsrisiko");
    }

    @Test
    void calculatesCorrectAverages() {
        List<SocialInsuranceEntry> entries = List.of(
                new SocialInsuranceEntry(user, 2026, 1,
                        new BigDecimal("40.0"), new BigDecimal("10.0"),
                        new BigDecimal("5000"), new BigDecimal("3000")),
                new SocialInsuranceEntry(user, 2026, 2,
                        new BigDecimal("40.0"), new BigDecimal("12.0"),
                        new BigDecimal("5000"), new BigDecimal("3500")),
                new SocialInsuranceEntry(user, 2026, 3,
                        new BigDecimal("40.0"), new BigDecimal("14.0"),
                        new BigDecimal("5000"), new BigDecimal("4000"))
        );
        when(entryRepository.findByUserIdAndYearOrderByMonthAsc(any(), eq((short) 2026)))
                .thenReturn(entries);

        SocialInsuranceStatus status = service.getStatus(1L, 2026);

        assertThat(status.avgEmploymentHoursWeekly()).isEqualByComparingTo("40.0");
        assertThat(status.avgSelfEmployedHoursWeekly()).isEqualByComparingTo("12.0");
        assertThat(status.totalEmploymentIncome()).isEqualByComparingTo("15000");
        assertThat(status.totalSelfEmployedIncome()).isEqualByComparingTo("10500");
        assertThat(status.monthlyData()).hasSize(3);
    }

    @Test
    void upsertCreatesNewEntry() {
        when(entryRepository.findByUserIdAndYearAndMonth(any(), eq((short) 2026), eq((short) 1)))
                .thenReturn(Optional.empty());
        when(entryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.upsertEntry(user, 2026, 1,
                new BigDecimal("40"), new BigDecimal("10"),
                new BigDecimal("5000"), new BigDecimal("3000"));

        verify(entryRepository).save(any(SocialInsuranceEntry.class));
    }

    @Test
    void upsertUpdatesExistingEntry() {
        SocialInsuranceEntry existing = new SocialInsuranceEntry(user, 2026, 1,
                new BigDecimal("35"), new BigDecimal("10"),
                new BigDecimal("4000"), new BigDecimal("2000"));
        when(entryRepository.findByUserIdAndYearAndMonth(any(), eq((short) 2026), eq((short) 1)))
                .thenReturn(Optional.of(existing));

        service.upsertEntry(user, 2026, 1,
                new BigDecimal("40"), new BigDecimal("15"),
                new BigDecimal("5000"), new BigDecimal("3000"));

        assertThat(existing.getEmploymentHoursWeekly()).isEqualByComparingTo("40");
        assertThat(existing.getSelfEmployedHoursWeekly()).isEqualByComparingTo("15");
        verify(entryRepository, never()).save(any());
    }
}
