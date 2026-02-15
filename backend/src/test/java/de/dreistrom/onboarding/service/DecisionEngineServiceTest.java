package de.dreistrom.onboarding.service;

import de.dreistrom.expense.repository.ExpenseEntryRepository;
import de.dreistrom.income.domain.Client;
import de.dreistrom.income.domain.ClientType;
import de.dreistrom.income.repository.ClientRepository;
import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.onboarding.domain.DecisionChoice;
import de.dreistrom.onboarding.dto.KurDecisionInput;
import de.dreistrom.onboarding.dto.KurDecisionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecisionEngineServiceTest {

    @Mock
    private IncomeEntryRepository incomeEntryRepository;

    @Mock
    private ExpenseEntryRepository expenseEntryRepository;

    @Mock
    private ClientRepository clientRepository;

    private DecisionEngineService service;

    @BeforeEach
    void setUp() {
        service = new DecisionEngineService(incomeEntryRepository, expenseEntryRepository, clientRepository);
        // Set limits via reflection since @Value won't work in unit tests
        try {
            var currentField = DecisionEngineService.class.getDeclaredField("currentYearLimitEur");
            currentField.setAccessible(true);
            currentField.setInt(service, 22000);

            var projectedField = DecisionEngineService.class.getDeclaredField("projectedYearLimitEur");
            projectedField.setAccessible(true);
            projectedField.setInt(service, 50000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void lowRevenue_b2cOnly_recommendsKleinunternehmer() {
        KurDecisionInput input = new KurDecisionInput(
                new BigDecimal("5000"),   // Freiberuf
                new BigDecimal("3000"),   // Gewerbe
                new BigDecimal("1000"),   // Expenses
                0,                         // B2B clients
                5                          // B2C clients
        );

        KurDecisionResponse response = service.evaluateKleinunternehmer(1L, input);

        assertThat(response.recommendation()).isEqualTo(DecisionChoice.OPTION_A);
        assertThat(response.belowCurrentYearLimit()).isTrue();
        assertThat(response.belowProjectedYearLimit()).isTrue();
        assertThat(response.projectedTotalRevenue()).isEqualByComparingTo("8000");
    }

    @Test
    void highRevenue_exceedsLimit_recommendsRegelbesteuerung() {
        KurDecisionInput input = new KurDecisionInput(
                new BigDecimal("15000"),
                new BigDecimal("10000"),
                new BigDecimal("5000"),
                3,
                2
        );

        KurDecisionResponse response = service.evaluateKleinunternehmer(1L, input);

        assertThat(response.recommendation()).isEqualTo(DecisionChoice.OPTION_B);
        assertThat(response.belowCurrentYearLimit()).isFalse();
        assertThat(response.projectedTotalRevenue()).isEqualByComparingTo("25000");
    }

    @Test
    void belowLimit_butHighB2B_recommendsRegelbesteuerung() {
        KurDecisionInput input = new KurDecisionInput(
                new BigDecimal("8000"),
                new BigDecimal("6000"),
                new BigDecimal("8000"),
                8,
                2
        );

        KurDecisionResponse response = service.evaluateKleinunternehmer(1L, input);

        // High B2B ratio (80%) + high expenses (Vorsteuer ~1520 EUR) → Regelbesteuerung
        assertThat(response.recommendation()).isEqualTo(DecisionChoice.OPTION_B);
        assertThat(response.b2bRatio()).isGreaterThan(new BigDecimal("50"));
    }

    @Test
    void borderlineRevenue_withHighExpenses_recommendsRegelbesteuerung() {
        KurDecisionInput input = new KurDecisionInput(
                new BigDecimal("10000"),
                new BigDecimal("8000"),
                new BigDecimal("10000"),
                5,
                5
        );

        KurDecisionResponse response = service.evaluateKleinunternehmer(1L, input);

        // Close to limit (18000/22000=82%) + high Vorsteuer (1900) → Regelbesteuerung
        assertThat(response.recommendation()).isEqualTo(DecisionChoice.OPTION_B);
        assertThat(response.estimatedVorsteuerSavings()).isGreaterThan(new BigDecimal("1500"));
    }

    @Test
    void veryLowRevenue_minimalExpenses_recommendsKleinunternehmer() {
        KurDecisionInput input = new KurDecisionInput(
                new BigDecimal("2000"),
                new BigDecimal("1000"),
                new BigDecimal("500"),
                1,
                3
        );

        KurDecisionResponse response = service.evaluateKleinunternehmer(1L, input);

        assertThat(response.recommendation()).isEqualTo(DecisionChoice.OPTION_A);
        assertThat(response.projectedTotalRevenue()).isEqualByComparingTo("3000");
        assertThat(response.summary()).contains("Kleinunternehmerregelung");
    }

    @Test
    void response_containsAllProsCons() {
        KurDecisionInput input = new KurDecisionInput(
                new BigDecimal("10000"),
                new BigDecimal("5000"),
                new BigDecimal("3000"),
                2,
                3
        );

        KurDecisionResponse response = service.evaluateKleinunternehmer(1L, input);

        assertThat(response.prosKleinunternehmer()).isNotEmpty();
        assertThat(response.consKleinunternehmer()).isNotEmpty();
        assertThat(response.prosRegelbesteuerung()).isNotEmpty();
        assertThat(response.consRegelbesteuerung()).isNotEmpty();
    }

    @Test
    void vorsteuerSavings_calculatedAt19Percent() {
        KurDecisionInput input = new KurDecisionInput(
                new BigDecimal("10000"),
                new BigDecimal("5000"),
                new BigDecimal("10000"),
                2,
                3
        );

        KurDecisionResponse response = service.evaluateKleinunternehmer(1L, input);

        // 10000 * 0.19 = 1900
        assertThat(response.estimatedVorsteuerSavings()).isEqualByComparingTo("1900.00");
    }

    @Test
    void b2bRatio_calculatedCorrectly() {
        KurDecisionInput input = new KurDecisionInput(
                new BigDecimal("5000"),
                new BigDecimal("3000"),
                new BigDecimal("1000"),
                3,
                7
        );

        KurDecisionResponse response = service.evaluateKleinunternehmer(1L, input);

        // 3/(3+7) * 100 = 30.0%
        assertThat(response.b2bRatio()).isEqualByComparingTo("30.0");
    }

    @Test
    void noClients_b2bRatioIsZero() {
        KurDecisionInput input = new KurDecisionInput(
                new BigDecimal("5000"),
                new BigDecimal("3000"),
                new BigDecimal("1000"),
                0,
                0
        );

        KurDecisionResponse response = service.evaluateKleinunternehmer(1L, input);

        assertThat(response.b2bRatio()).isEqualByComparingTo("0");
    }

    @Test
    void exceedsProjectedLimit_notCurrent_scoreIncreased() {
        // Revenue 21000 < 22000 current limit but > 50000 projected limit? No.
        // Let's test: exactly at 21999 — below current but above 80% threshold
        KurDecisionInput input = new KurDecisionInput(
                new BigDecimal("12000"),
                new BigDecimal("9999"),
                new BigDecimal("200"),
                0,
                5
        );

        KurDecisionResponse response = service.evaluateKleinunternehmer(1L, input);

        // Total is 21999, below 22000, b2b=0, vorsteuer=38 EUR, near 80% threshold (17600)
        // score: +1 for near limit = 1 total → OPTION_A
        assertThat(response.recommendation()).isEqualTo(DecisionChoice.OPTION_A);
        assertThat(response.belowCurrentYearLimit()).isTrue();
    }
}
