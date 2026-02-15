package de.dreistrom.onboarding.service;

import de.dreistrom.expense.repository.ExpenseEntryRepository;
import de.dreistrom.income.domain.ClientType;
import de.dreistrom.income.repository.ClientRepository;
import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.onboarding.domain.DecisionChoice;
import de.dreistrom.onboarding.dto.KurDecisionInput;
import de.dreistrom.onboarding.dto.KurDecisionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DecisionEngineService {

    private static final BigDecimal VAT_RATE = new BigDecimal("0.19");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Value("${dreistrom.vat.kleinunternehmer.current-year-limit:22000}")
    private int currentYearLimitEur;

    @Value("${dreistrom.vat.kleinunternehmer.projected-year-limit:50000}")
    private int projectedYearLimitEur;

    private final IncomeEntryRepository incomeEntryRepository;
    private final ExpenseEntryRepository expenseEntryRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public KurDecisionResponse evaluateKleinunternehmer(Long userId, KurDecisionInput input) {
        BigDecimal projectedTotal = input.projectedFreiberufRevenue()
                .add(input.projectedGewerbeRevenue());
        BigDecimal currentLimit = BigDecimal.valueOf(currentYearLimitEur);
        BigDecimal projectedLimit = BigDecimal.valueOf(projectedYearLimitEur);

        boolean belowCurrent = projectedTotal.compareTo(currentLimit) <= 0;
        boolean belowProjected = projectedTotal.compareTo(projectedLimit) <= 0;

        int totalClients = input.b2bClientCount() + input.b2cClientCount();
        BigDecimal b2bRatio = totalClients > 0
                ? BigDecimal.valueOf(input.b2bClientCount())
                    .divide(BigDecimal.valueOf(totalClients), 4, RoundingMode.HALF_UP)
                    .multiply(HUNDRED).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal estimatedVorsteuer = input.projectedBusinessExpenses()
                .multiply(VAT_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        List<String> prosKur = buildProsKleinunternehmer(belowCurrent, estimatedVorsteuer, b2bRatio);
        List<String> consKur = buildConsKleinunternehmer(estimatedVorsteuer, b2bRatio, belowCurrent);
        List<String> prosRegel = buildProsRegelbesteuerung(estimatedVorsteuer, b2bRatio);
        List<String> consRegel = buildConsRegelbesteuerung(estimatedVorsteuer);

        DecisionChoice recommendation = computeRecommendation(
                belowCurrent, belowProjected, estimatedVorsteuer, b2bRatio, projectedTotal);
        String summary = buildSummary(recommendation, projectedTotal, currentLimit, estimatedVorsteuer, b2bRatio);

        return new KurDecisionResponse(
                recommendation,
                summary,
                projectedTotal,
                belowCurrent,
                belowProjected,
                estimatedVorsteuer,
                b2bRatio,
                prosKur,
                consKur,
                prosRegel,
                consRegel
        );
    }

    @Transactional(readOnly = true)
    public KurDecisionResponse evaluateFromActualData(Long userId, int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        Long freiberufCents = incomeEntryRepository.sumCentsByStreamAndDateRange(
                userId, "FREIBERUF", yearStart, yearEnd);
        Long gewerbeCents = incomeEntryRepository.sumCentsByStreamAndDateRange(
                userId, "GEWERBE", yearStart, yearEnd);

        Long expenseCents = expenseEntryRepository.sumCentsByDateRange(userId, yearStart, yearEnd);

        long b2bCount = clientRepository.findByUserIdAndActiveTrue(userId).stream()
                .filter(c -> c.getClientType() == ClientType.B2B).count();
        long b2cCount = clientRepository.findByUserIdAndActiveTrue(userId).stream()
                .filter(c -> c.getClientType() == ClientType.B2C).count();

        BigDecimal freiberufRevenue = centsToEur(freiberufCents);
        BigDecimal gewerbeRevenue = centsToEur(gewerbeCents);
        BigDecimal businessExpenses = centsToEur(expenseCents);

        LocalDate today = LocalDate.now();
        if (today.getYear() == year) {
            int dayOfYear = today.getDayOfYear();
            int daysInYear = today.lengthOfYear();
            BigDecimal factor = BigDecimal.valueOf(daysInYear)
                    .divide(BigDecimal.valueOf(Math.max(dayOfYear, 1)), 4, RoundingMode.HALF_UP);
            freiberufRevenue = freiberufRevenue.multiply(factor).setScale(2, RoundingMode.HALF_UP);
            gewerbeRevenue = gewerbeRevenue.multiply(factor).setScale(2, RoundingMode.HALF_UP);
            businessExpenses = businessExpenses.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        }

        KurDecisionInput input = new KurDecisionInput(
                freiberufRevenue, gewerbeRevenue, businessExpenses,
                (int) b2bCount, (int) b2cCount);

        return evaluateKleinunternehmer(userId, input);
    }

    private DecisionChoice computeRecommendation(boolean belowCurrent, boolean belowProjected,
                                                  BigDecimal vorsteuer, BigDecimal b2bRatio,
                                                  BigDecimal projectedTotal) {
        if (!belowCurrent) {
            return DecisionChoice.OPTION_B;
        }

        int score = 0;

        if (b2bRatio.compareTo(new BigDecimal("50")) >= 0) {
            score += 2;
        }

        if (vorsteuer.compareTo(new BigDecimal("1000")) >= 0) {
            score += 2;
        } else if (vorsteuer.compareTo(new BigDecimal("500")) >= 0) {
            score += 1;
        }

        BigDecimal eightyPctLimit = BigDecimal.valueOf(currentYearLimitEur)
                .multiply(new BigDecimal("0.80"));
        if (projectedTotal.compareTo(eightyPctLimit) >= 0) {
            score += 1;
        }

        if (!belowProjected) {
            score += 2;
        }

        return score >= 3 ? DecisionChoice.OPTION_B : DecisionChoice.OPTION_A;
    }

    private List<String> buildProsKleinunternehmer(boolean belowLimit, BigDecimal vorsteuer,
                                                    BigDecimal b2bRatio) {
        List<String> pros = new ArrayList<>();
        pros.add("Keine Umsatzsteuer auf Rechnungen — einfachere Preisgestaltung für Endkunden");
        pros.add("Deutlich reduzierter Buchhaltungsaufwand — keine USt-Voranmeldungen nötig");
        if (belowLimit) {
            pros.add("Projizierter Umsatz liegt unter der Grenze von "
                    + currentYearLimitEur + " EUR — Berechtigung gegeben");
        }
        if (b2bRatio.compareTo(new BigDecimal("30")) < 0) {
            pros.add("Überwiegend B2C-Kunden — diese profitieren von niedrigeren Bruttopreisen");
        }
        return pros;
    }

    private List<String> buildConsKleinunternehmer(BigDecimal vorsteuer, BigDecimal b2bRatio,
                                                    boolean belowLimit) {
        List<String> cons = new ArrayList<>();
        cons.add("Kein Vorsteuerabzug auf Betriebsausgaben (geschätzter Verlust: "
                + vorsteuer + " EUR/Jahr)");
        if (b2bRatio.compareTo(new BigDecimal("50")) >= 0) {
            cons.add("B2B-Kunden erwarten USt-Ausweis auf Rechnungen — kann unprofessionell wirken");
        }
        if (!belowLimit) {
            cons.add("Projizierter Umsatz übersteigt die Grenze — Kleinunternehmerregelung nicht anwendbar");
        }
        cons.add("Späterer Wechsel zur Regelbesteuerung bindet für 5 Jahre (§19 Abs. 2 UStG)");
        return cons;
    }

    private List<String> buildProsRegelbesteuerung(BigDecimal vorsteuer, BigDecimal b2bRatio) {
        List<String> pros = new ArrayList<>();
        pros.add("Vorsteuerabzug auf alle Betriebsausgaben (geschätzte Ersparnis: "
                + vorsteuer + " EUR/Jahr)");
        if (b2bRatio.compareTo(new BigDecimal("50")) >= 0) {
            pros.add("Professionelles Auftreten bei B2B-Kunden mit ordnungsgemäßem USt-Ausweis");
        }
        pros.add("Keine Umsatzgrenze — Wachstum ohne Systemwechsel möglich");
        pros.add("USt-IdNr ermöglicht reibungslose EU-Geschäfte (Reverse Charge)");
        return pros;
    }

    private List<String> buildConsRegelbesteuerung(BigDecimal vorsteuer) {
        List<String> cons = new ArrayList<>();
        cons.add("Monatliche oder vierteljährliche USt-Voranmeldung erforderlich");
        cons.add("Höherer Buchhaltungsaufwand und ggf. Steuerberaterkosten");
        if (vorsteuer.compareTo(new BigDecimal("500")) < 0) {
            cons.add("Geringe Betriebsausgaben — Vorsteuererstattung fällt gering aus");
        }
        cons.add("B2C-Endpreise sind 19% höher (USt wird aufgeschlagen)");
        return cons;
    }

    private String buildSummary(DecisionChoice recommendation, BigDecimal projectedTotal,
                                 BigDecimal limit, BigDecimal vorsteuer, BigDecimal b2bRatio) {
        if (recommendation == DecisionChoice.OPTION_A) {
            return String.format(
                    "Empfehlung: Kleinunternehmerregelung anwenden. "
                    + "Ihr projizierter Umsatz (%s EUR) liegt unter der Grenze von %s EUR. "
                    + "Der Vorsteuer-Vorteil bei Regelbesteuerung wäre mit ca. %s EUR/Jahr gering.",
                    projectedTotal.setScale(0, RoundingMode.HALF_UP),
                    limit.setScale(0, RoundingMode.HALF_UP),
                    vorsteuer);
        } else {
            return String.format(
                    "Empfehlung: Regelbesteuerung wählen. "
                    + "Bei einem projizierten Umsatz von %s EUR und einem B2B-Anteil von %s%% "
                    + "überwiegen die Vorteile des Vorsteuerabzugs (%s EUR/Jahr) "
                    + "und des professionellen Auftretens.",
                    projectedTotal.setScale(0, RoundingMode.HALF_UP),
                    b2bRatio,
                    vorsteuer);
        }
    }

    private BigDecimal centsToEur(Long cents) {
        if (cents == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(cents).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
