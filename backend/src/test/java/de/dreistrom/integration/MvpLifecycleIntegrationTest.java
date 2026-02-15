package de.dreistrom.integration;

import de.dreistrom.audit.domain.EventLog;
import de.dreistrom.audit.repository.EventLogRepository;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.config.RateLimitFilter;
import de.dreistrom.expense.domain.AllocationRule;
import de.dreistrom.expense.domain.ExpenseEntry;
import de.dreistrom.expense.repository.AllocationRuleRepository;
import de.dreistrom.expense.repository.DepreciationAssetRepository;
import de.dreistrom.expense.repository.ExpenseEntryRepository;
import de.dreistrom.expense.service.ExpenseService;
import de.dreistrom.income.domain.Client;
import de.dreistrom.income.domain.ClientType;
import de.dreistrom.income.domain.IncomeEntry;
import de.dreistrom.income.repository.ClientRepository;
import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.income.service.IncomeService;
import de.dreistrom.invoicing.domain.*;
import de.dreistrom.invoicing.repository.InvoiceRepository;
import de.dreistrom.invoicing.repository.InvoiceSequenceRepository;
import de.dreistrom.invoicing.service.InvoiceService;
import de.dreistrom.onboarding.domain.DecisionChoice;
import de.dreistrom.onboarding.domain.StepStatus;
import de.dreistrom.onboarding.dto.OnboardingProgressResponse;
import de.dreistrom.onboarding.repository.DecisionPointRepository;
import de.dreistrom.onboarding.repository.RegistrationStepRepository;
import de.dreistrom.onboarding.service.OnboardingService;
import de.dreistrom.tax.dto.*;
import de.dreistrom.tax.service.AnnualTaxExportService;
import de.dreistrom.tax.service.AnnualTaxPackageService;
import de.dreistrom.tax.service.EuerService;
import de.dreistrom.vat.dto.VatSummary;
import de.dreistrom.vat.service.VatCalculationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full MVP lifecycle integration test — exercises the entire business flow:
 * <ol>
 *   <li>User registration and onboarding</li>
 *   <li>Income entry across all 3 streams (Employment, Freiberuf, Gewerbe)</li>
 *   <li>Expense allocation with stream split</li>
 *   <li>Invoice generation (Freiberuf + Gewerbe)</li>
 *   <li>VAT calculation (UStVA)</li>
 *   <li>EÜR generation per stream</li>
 *   <li>Annual tax package assembly</li>
 *   <li>ELSTER XML + CSV export</li>
 *   <li>GoBD audit trail verification</li>
 * </ol>
 *
 * Uses H2 in-memory database with Hibernate DDL (no Flyway).
 * All steps run in a single @Transactional context.
 */
@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MvpLifecycleIntegrationTest {

    // ── Services ──────────────────────────────────────────────────────────
    @Autowired private OnboardingService onboardingService;
    @Autowired private IncomeService incomeService;
    @Autowired private ExpenseService expenseService;
    @Autowired private InvoiceService invoiceService;
    @Autowired private VatCalculationService vatCalculationService;
    @Autowired private EuerService euerService;
    @Autowired private AnnualTaxPackageService annualTaxPackageService;
    @Autowired private AnnualTaxExportService annualTaxExportService;

    // ── Repositories ──────────────────────────────────────────────────────
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private IncomeEntryRepository incomeEntryRepository;
    @Autowired private ExpenseEntryRepository expenseEntryRepository;
    @Autowired private AllocationRuleRepository allocationRuleRepository;
    @Autowired private DepreciationAssetRepository depreciationAssetRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private InvoiceSequenceRepository invoiceSequenceRepository;
    @Autowired private RegistrationStepRepository stepRepository;
    @Autowired private DecisionPointRepository decisionPointRepository;
    @Autowired private EventLogRepository eventLogRepository;

    // ── Misc ──────────────────────────────────────────────────────────────
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RateLimitFilter rateLimitFilter;

    // ── Test fixtures ─────────────────────────────────────────────────────
    private static final int TAX_YEAR = 2025;
    private static final LocalDate JAN_15 = LocalDate.of(TAX_YEAR, 1, 15);
    private static final LocalDate MAR_01 = LocalDate.of(TAX_YEAR, 3, 1);
    private static final LocalDate JUN_01 = LocalDate.of(TAX_YEAR, 6, 1);
    private static final LocalDate SEP_15 = LocalDate.of(TAX_YEAR, 9, 15);
    private static final LocalDate YEAR_START = LocalDate.of(TAX_YEAR, 1, 1);
    private static final LocalDate YEAR_END = LocalDate.of(TAX_YEAR, 12, 31);

    private AppUser user;
    private Client freiberufClient;
    private Client gewerbeClient;
    private AllocationRule splitRule;

    @BeforeEach
    void setUp() {
        rateLimitFilter.clearBuckets();

        // Create test user
        user = appUserRepository.save(new AppUser(
                "lifecycle@dreistrom.de",
                passwordEncoder.encode("Test1234!"),
                "Max Mustermann"));

        // Set security context for audit logging
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "lifecycle@dreistrom.de", null, List.of()));

        // Create clients
        freiberufClient = clientRepository.save(new Client(
                user, "Freelance Kunde GmbH", IncomeStream.FREIBERUF,
                ClientType.B2B, "DE", "DE123456789"));
        gewerbeClient = clientRepository.save(new Client(
                user, "Handels Kunde AG", IncomeStream.GEWERBE,
                ClientType.B2B, "DE", "DE987654321"));

        // Create 60/40 allocation rule (Freiberuf/Gewerbe)
        splitRule = allocationRuleRepository.save(new AllocationRule(
                user, "Standard 60/40", (short) 60, (short) 40, (short) 0));
    }

    // ═════════════════════════════════════════════════════════════════════
    //  PHASE 1 — Onboarding
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    void phase1_onboarding_initializes_15_steps_and_records_decision() {
        // Initialize checklist
        onboardingService.initializeChecklist(user);
        OnboardingProgressResponse progress = onboardingService.getProgress(user.getId());

        assertThat(progress.totalSteps()).isEqualTo(15);
        assertThat(progress.completedSteps()).isZero();
        assertThat(progress.progressPercent()).isZero();

        // Steps 1 (no deps) should be startable
        var step1 = onboardingService.startStep(user.getId(), 1);
        assertThat(step1.status()).isEqualTo(StepStatus.IN_PROGRESS);

        var completed1 = onboardingService.completeStep(user.getId(), 1);
        assertThat(completed1.status()).isEqualTo(StepStatus.COMPLETED);

        // Complete prerequisites for step 4 (Kleinunternehmerregelung)
        onboardingService.startStep(user.getId(), 2);
        onboardingService.completeStep(user.getId(), 2);
        onboardingService.startStep(user.getId(), 3);
        onboardingService.completeStep(user.getId(), 3);

        // Step 4 should have a decision point
        var step4 = onboardingService.startStep(user.getId(), 4);
        assertThat(step4.decisionPoints()).isNotEmpty();

        var dp = step4.decisionPoints().get(0);
        assertThat(dp.question()).contains("Kleinunternehmerregelung");

        // Choose Regelbesteuerung (OPTION_B — standard VAT)
        var decision = onboardingService.makeDecision(user.getId(), dp.id(), DecisionChoice.OPTION_B);
        assertThat(decision.userChoice()).isEqualTo(DecisionChoice.OPTION_B);

        onboardingService.completeStep(user.getId(), 4);

        // Verify progress updated
        var updatedProgress = onboardingService.getProgress(user.getId());
        assertThat(updatedProgress.completedSteps()).isEqualTo(4);
        assertThat(updatedProgress.progressPercent()).isGreaterThan(0);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  PHASE 2 — Income Entry (All 3 Streams)
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(2)
    void phase2_income_entries_across_three_streams() {
        // Employment income (W2 salary)
        IncomeEntry salary = incomeService.create(user, IncomeStream.EMPLOYMENT,
                new BigDecimal("3500.00"), JAN_15,
                "Monatliches Gehalt", null, "Angestelltenvergütung Januar");
        assertThat(salary.getId()).isNotNull();
        assertThat(salary.getStreamType()).isEqualTo(IncomeStream.EMPLOYMENT);

        // Freiberuf income (consulting)
        IncomeEntry consulting = incomeService.create(user, IncomeStream.FREIBERUF,
                new BigDecimal("5000.00"), MAR_01,
                "IT-Beratung", freiberufClient, "Beratungsleistung Q1");
        assertThat(consulting.getStreamType()).isEqualTo(IncomeStream.FREIBERUF);

        // Gewerbe income (trade)
        IncomeEntry trade = incomeService.create(user, IncomeStream.GEWERBE,
                new BigDecimal("8000.00"), JUN_01,
                "Warenverkauf", gewerbeClient, "Handelseinnahmen H1");
        assertThat(trade.getStreamType()).isEqualTo(IncomeStream.GEWERBE);

        // Verify all entries persisted
        List<IncomeEntry> all = incomeService.listAll(user.getId());
        assertThat(all).hasSize(3);

        // Verify stream filtering
        assertThat(incomeService.listByStream(user.getId(), IncomeStream.EMPLOYMENT)).hasSize(1);
        assertThat(incomeService.listByStream(user.getId(), IncomeStream.FREIBERUF)).hasSize(1);
        assertThat(incomeService.listByStream(user.getId(), IncomeStream.GEWERBE)).hasSize(1);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  PHASE 3 — Expense Allocation
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    void phase3_expense_creation_with_stream_allocation() {
        // Shared business expense (software subscription)
        ExpenseEntry software = expenseService.create(user,
                new BigDecimal("500.00"), "Software",
                MAR_01, splitRule.getId(), null, "JetBrains-Lizenz");
        assertThat(software.getId()).isNotNull();
        assertThat(software.getAllocationRule()).isNotNull();
        assertThat(software.getAllocationRule().getFreiberufPct()).isEqualTo((short) 60);

        // Office rent (shared expense)
        ExpenseEntry rent = expenseService.create(user,
                new BigDecimal("1200.00"), "Miete",
                MAR_01, splitRule.getId(), null, "Büroanteil März");
        assertThat(rent.getId()).isNotNull();

        // Travel expense (no allocation — 100% business)
        ExpenseEntry travel = expenseService.create(user,
                new BigDecimal("350.00"), "Reisekosten",
                SEP_15, null, null, "Kundenbesuch Berlin");
        assertThat(travel.getId()).isNotNull();

        // Verify all expenses persisted
        assertThat(expenseService.listAll(user.getId())).hasSize(3);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  PHASE 4 — Invoice Generation
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(4)
    void phase4_invoice_creation_and_lifecycle() {
        // Freiberuf invoice (consulting)
        List<LineItem> fbLineItems = List.of(
                new LineItem("IT-Beratung 40h", new BigDecimal("40"), new BigDecimal("125.00"),
                        new BigDecimal("19")));
        BigDecimal fbNet = new BigDecimal("5000.00");
        BigDecimal fbVat = new BigDecimal("950.00");
        BigDecimal fbGross = new BigDecimal("5950.00");

        Invoice fbInvoice = invoiceService.create(user, InvoiceStream.FREIBERUF,
                freiberufClient.getId(), MAR_01, MAR_01.plusDays(30),
                fbLineItems, fbNet, fbVat, fbGross,
                VatTreatment.REGULAR, null);
        assertThat(fbInvoice.getId()).isNotNull();
        assertThat(fbInvoice.getNumber()).startsWith("FR-");
        assertThat(fbInvoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(fbInvoice.getVat()).isEqualByComparingTo(fbVat);

        // Mark as sent → paid
        invoiceService.updateStatus(fbInvoice.getId(), user.getId(), InvoiceStatus.SENT);
        Invoice sent = invoiceService.getById(fbInvoice.getId(), user.getId());
        assertThat(sent.getStatus()).isEqualTo(InvoiceStatus.SENT);

        invoiceService.updateStatus(fbInvoice.getId(), user.getId(), InvoiceStatus.PAID);
        Invoice paid = invoiceService.getById(fbInvoice.getId(), user.getId());
        assertThat(paid.getStatus()).isEqualTo(InvoiceStatus.PAID);

        // Gewerbe invoice (trade)
        List<LineItem> gwLineItems = List.of(
                new LineItem("Warenlieferung", new BigDecimal("1"), new BigDecimal("8000.00"),
                        new BigDecimal("19")));
        BigDecimal gwNet = new BigDecimal("8000.00");
        BigDecimal gwVat = new BigDecimal("1520.00");
        BigDecimal gwGross = new BigDecimal("9520.00");

        Invoice gwInvoice = invoiceService.create(user, InvoiceStream.GEWERBE,
                gewerbeClient.getId(), JUN_01, JUN_01.plusDays(30),
                gwLineItems, gwNet, gwVat, gwGross,
                VatTreatment.REGULAR, null);
        assertThat(gwInvoice.getId()).isNotNull();
        assertThat(gwInvoice.getNumber()).startsWith("GW-");

        // Verify income entries created automatically by invoice service
        List<IncomeEntry> incomes = incomeService.listAll(user.getId());
        assertThat(incomes).hasSizeGreaterThanOrEqualTo(2);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  PHASE 5 — VAT Calculation
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(5)
    void phase5_vat_calculation() {
        // Setup: create invoices and expenses
        createInvoicesAndExpenses();

        VatSummary vat = vatCalculationService.calculate(
                user.getId(), YEAR_START, YEAR_END, false);

        assertThat(vat.kleinunternehmer()).isFalse();

        // Output VAT: from invoices (Freiberuf + Gewerbe)
        assertThat(vat.outputVat()).isGreaterThan(BigDecimal.ZERO);
        assertThat(vat.freiberufOutputVat()).isGreaterThan(BigDecimal.ZERO);
        assertThat(vat.gewerbeOutputVat()).isGreaterThan(BigDecimal.ZERO);

        // Input VAT: calculated from allocated expenses
        assertThat(vat.inputVat()).isGreaterThanOrEqualTo(BigDecimal.ZERO);

        // Net payable = output - input
        BigDecimal expectedNet = vat.outputVat().subtract(vat.inputVat());
        assertThat(vat.netPayable()).isEqualByComparingTo(expectedNet);

        // Verify Kleinunternehmer mode zeroes everything
        VatSummary kurVat = vatCalculationService.calculate(
                user.getId(), YEAR_START, YEAR_END, true);
        assertThat(kurVat.outputVat()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(kurVat.inputVat()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(kurVat.netPayable()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  PHASE 6 — EÜR Generation
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(6)
    void phase6_euer_generation_per_stream() {
        createInvoicesAndExpenses();

        // Freiberuf EÜR
        EuerResult euerFb = euerService.generate(user.getId(), IncomeStream.FREIBERUF, TAX_YEAR);
        assertThat(euerFb.taxYear()).isEqualTo(TAX_YEAR);
        assertThat(euerFb.stream()).isEqualTo(IncomeStream.FREIBERUF);
        assertThat(euerFb.totalIncome()).isGreaterThan(BigDecimal.ZERO);
        assertThat(euerFb.profit()).isNotNull();

        // Gewerbe EÜR
        EuerResult euerGw = euerService.generate(user.getId(), IncomeStream.GEWERBE, TAX_YEAR);
        assertThat(euerGw.stream()).isEqualTo(IncomeStream.GEWERBE);
        assertThat(euerGw.totalIncome()).isGreaterThan(BigDecimal.ZERO);
        assertThat(euerGw.profit()).isNotNull();

        // Dual-stream EÜR
        EuerService.DualStreamEuer dual = euerService.generateDual(user.getId(), TAX_YEAR);
        assertThat(dual.freiberuf().profit()).isEqualByComparingTo(euerFb.profit());
        assertThat(dual.gewerbe().profit()).isEqualByComparingTo(euerGw.profit());
        assertThat(dual.combinedProfit())
                .isEqualByComparingTo(euerFb.profit().add(euerGw.profit()));
    }

    // ═════════════════════════════════════════════════════════════════════
    //  PHASE 7 — Tax Package Assembly & Export
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(7)
    void phase7_annual_tax_package_assembly() {
        createFullDataSet();

        AnnualTaxPackage pkg = annualTaxPackageService.assemble(user.getId(), TAX_YEAR);

        assertThat(pkg.taxYear()).isEqualTo(TAX_YEAR);

        // Anlage N (Employment)
        assertThat(pkg.anlageN()).isNotNull();
        assertThat(pkg.anlageN().bruttoarbeitslohn()).isGreaterThan(BigDecimal.ZERO);

        // Anlage S (Freiberuf)
        assertThat(pkg.anlageS()).isNotNull();
        assertThat(pkg.anlageS().einnahmen()).isGreaterThan(BigDecimal.ZERO);

        // Anlage G (Gewerbe)
        assertThat(pkg.anlageG()).isNotNull();
        assertThat(pkg.anlageG().einnahmen()).isGreaterThan(BigDecimal.ZERO);

        // EÜR results
        assertThat(pkg.euerFreiberuf()).isNotNull();
        assertThat(pkg.euerGewerbe()).isNotNull();

        // Tax totals
        assertThat(pkg.taxCalculation()).isNotNull();
        assertThat(pkg.taxCalculation().taxableIncome()).isNotNull();
        assertThat(pkg.gewerbesteuer()).isNotNull();

        // Vorsorgeaufwand
        assertThat(pkg.vorsorgeaufwand()).isNotNull();
    }

    @Test
    @Order(8)
    void phase7b_elster_xml_export() {
        createFullDataSet();

        AnnualTaxPackage pkg = annualTaxPackageService.assemble(user.getId(), TAX_YEAR);
        byte[] xml = annualTaxExportService.generateElsterXml(pkg, user.getDisplayName());

        assertThat(xml).isNotEmpty();
        String xmlStr = new String(xml);
        assertThat(xmlStr).contains("<?xml");
        assertThat(xmlStr).contains(String.valueOf(TAX_YEAR));
        assertThat(xmlStr).contains("Max Mustermann");
        assertThat(xmlStr).contains("Einkommensteuer");
    }

    @Test
    @Order(9)
    void phase7c_csv_export() {
        createFullDataSet();

        AnnualTaxPackage pkg = annualTaxPackageService.assemble(user.getId(), TAX_YEAR);
        byte[] csv = annualTaxExportService.generateCsv(pkg);

        assertThat(csv).isNotEmpty();
        String csvStr = new String(csv);
        assertThat(csvStr).contains("Anlage;Position;Betrag EUR");
        assertThat(csvStr).contains("Anlage N;Bruttoarbeitslohn;");
        assertThat(csvStr).contains("Anlage S;Einnahmen;");
        assertThat(csvStr).contains("Anlage G;Einnahmen;");
        assertThat(csvStr).contains("EUeR Freiberuf;Betriebseinnahmen;");
        assertThat(csvStr).contains("EUeR Gewerbe;Betriebseinnahmen;");
        assertThat(csvStr).contains("Festsetzung;Einkommensteuer;");
    }

    // ═════════════════════════════════════════════════════════════════════
    //  PHASE 8 — GoBD Audit Trail Verification
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(10)
    void phase8_gobd_audit_trail_complete() {
        // Create income entry and verify audit event
        IncomeEntry salary = incomeService.create(user, IncomeStream.EMPLOYMENT,
                new BigDecimal("4000.00"), JAN_15,
                "Gehalt", null, "Januar-Gehalt");

        List<EventLog> incomeEvents = eventLogRepository
                .findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        "IncomeEntry", salary.getId());
        assertThat(incomeEvents).isNotEmpty();
        assertThat(incomeEvents.get(0).getEventType()).contains("CREATED");
        assertThat(incomeEvents.get(0).getActor()).isEqualTo("lifecycle@dreistrom.de");
        assertThat(incomeEvents.get(0).getPayload()).isNotBlank();

        // Create expense and verify audit event
        ExpenseEntry expense = expenseService.create(user,
                new BigDecimal("200.00"), "Büromaterial",
                MAR_01, null, null, "Druckerpapier");
        List<EventLog> expenseEvents = eventLogRepository
                .findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        "ExpenseEntry", expense.getId());
        assertThat(expenseEvents).isNotEmpty();
        assertThat(expenseEvents.get(0).getEventType()).contains("CREATED");

        // Create invoice and verify audit event
        Invoice invoice = invoiceService.create(user, InvoiceStream.FREIBERUF,
                freiberufClient.getId(), MAR_01, MAR_01.plusDays(30),
                List.of(new LineItem("Beratung", BigDecimal.ONE, new BigDecimal("1000.00"),
                        new BigDecimal("19"))),
                new BigDecimal("1000.00"), new BigDecimal("190.00"),
                new BigDecimal("1190.00"), VatTreatment.REGULAR, null);
        List<EventLog> invoiceEvents = eventLogRepository
                .findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        "Invoice", invoice.getId());
        assertThat(invoiceEvents).isNotEmpty();
        assertThat(invoiceEvents.get(0).getEventType()).contains("CREATED");

        // Verify actor is consistently the authenticated user
        List<EventLog> allEvents = eventLogRepository.findAll();
        assertThat(allEvents).allSatisfy(event ->
                assertThat(event.getActor()).isEqualTo("lifecycle@dreistrom.de"));
    }

    // ═════════════════════════════════════════════════════════════════════
    //  PHASE 9 — Data Integrity Cross-Checks
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(11)
    void phase9_data_integrity_cross_module_consistency() {
        createFullDataSet();

        // Income entries by stream should match expected counts
        assertThat(incomeService.listByStream(user.getId(), IncomeStream.EMPLOYMENT)).isNotEmpty();
        assertThat(incomeService.listByStream(user.getId(), IncomeStream.FREIBERUF)).isNotEmpty();
        assertThat(incomeService.listByStream(user.getId(), IncomeStream.GEWERBE)).isNotEmpty();

        // Invoices should have matching income entries (created automatically)
        List<Invoice> invoices = invoiceRepository.findByUserId(user.getId());
        assertThat(invoices).hasSizeGreaterThanOrEqualTo(2);

        // EÜR income should match invoice-generated income entries
        EuerResult fbEuer = euerService.generate(user.getId(), IncomeStream.FREIBERUF, TAX_YEAR);
        assertThat(fbEuer.totalIncome()).isGreaterThan(BigDecimal.ZERO);

        EuerResult gwEuer = euerService.generate(user.getId(), IncomeStream.GEWERBE, TAX_YEAR);
        assertThat(gwEuer.totalIncome()).isGreaterThan(BigDecimal.ZERO);

        // Tax package should be consistent with individual components
        AnnualTaxPackage pkg = annualTaxPackageService.assemble(user.getId(), TAX_YEAR);
        assertThat(pkg.euerFreiberuf().totalIncome())
                .isEqualByComparingTo(fbEuer.totalIncome());
        assertThat(pkg.euerGewerbe().totalIncome())
                .isEqualByComparingTo(gwEuer.totalIncome());

        // Anlage S income = Freiberuf EÜR income
        assertThat(pkg.anlageS().einnahmen())
                .isEqualByComparingTo(fbEuer.totalIncome());
        // Anlage G income = Gewerbe EÜR income
        assertThat(pkg.anlageG().einnahmen())
                .isEqualByComparingTo(gwEuer.totalIncome());
    }

    // ═════════════════════════════════════════════════════════════════════
    //  FULL END-TO-END LIFECYCLE (single test covering all phases)
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(12)
    void fullLifecycle_onboarding_through_export() {
        // ── 1. Onboarding ─────────────────────────────────────────────
        onboardingService.initializeChecklist(user);
        OnboardingProgressResponse progress = onboardingService.getProgress(user.getId());
        assertThat(progress.totalSteps()).isEqualTo(15);

        // ── 2. Income: 3 streams ──────────────────────────────────────
        incomeService.create(user, IncomeStream.EMPLOYMENT,
                new BigDecimal("42000.00"), JAN_15, "Jahresgehalt", null, "Bruttolohn");
        incomeService.create(user, IncomeStream.FREIBERUF,
                new BigDecimal("15000.00"), MAR_01, "Consulting", freiberufClient, "Q1 Beratung");
        incomeService.create(user, IncomeStream.GEWERBE,
                new BigDecimal("25000.00"), JUN_01, "Handel", gewerbeClient, "H1 Umsatz");
        assertThat(incomeService.listAll(user.getId())).hasSize(3);

        // ── 3. Expenses with allocation ────────────────────────────────
        expenseService.create(user, new BigDecimal("600.00"), "Software",
                MAR_01, splitRule.getId(), null, "Lizenzen");
        expenseService.create(user, new BigDecimal("2400.00"), "Miete",
                MAR_01, splitRule.getId(), null, "Büroanteil");
        assertThat(expenseService.listAll(user.getId())).hasSize(2);

        // ── 4. Invoices ───────────────────────────────────────────────
        Invoice fbInv = invoiceService.create(user, InvoiceStream.FREIBERUF,
                freiberufClient.getId(), MAR_01, MAR_01.plusDays(30),
                List.of(new LineItem("Beratung Q1", new BigDecimal("1"),
                        new BigDecimal("15000.00"), new BigDecimal("19"))),
                new BigDecimal("15000.00"), new BigDecimal("2850.00"),
                new BigDecimal("17850.00"), VatTreatment.REGULAR, null);
        assertThat(fbInv.getNumber()).startsWith("FR-");

        Invoice gwInv = invoiceService.create(user, InvoiceStream.GEWERBE,
                gewerbeClient.getId(), JUN_01, JUN_01.plusDays(30),
                List.of(new LineItem("Warenverkauf H1", new BigDecimal("1"),
                        new BigDecimal("25000.00"), new BigDecimal("19"))),
                new BigDecimal("25000.00"), new BigDecimal("4750.00"),
                new BigDecimal("29750.00"), VatTreatment.REGULAR, null);
        assertThat(gwInv.getNumber()).startsWith("GW-");

        // ── 5. VAT ───────────────────────────────────────────────────
        VatSummary vat = vatCalculationService.calculate(
                user.getId(), YEAR_START, YEAR_END, false);
        assertThat(vat.outputVat()).isGreaterThan(BigDecimal.ZERO);

        // ── 6. EÜR ───────────────────────────────────────────────────
        EuerService.DualStreamEuer dual = euerService.generateDual(user.getId(), TAX_YEAR);
        assertThat(dual.freiberuf().totalIncome()).isGreaterThan(BigDecimal.ZERO);
        assertThat(dual.gewerbe().totalIncome()).isGreaterThan(BigDecimal.ZERO);

        // ── 7. Tax Package ────────────────────────────────────────────
        AnnualTaxPackage pkg = annualTaxPackageService.assemble(user.getId(), TAX_YEAR);
        assertThat(pkg.taxYear()).isEqualTo(TAX_YEAR);
        assertThat(pkg.anlageN().bruttoarbeitslohn()).isGreaterThan(BigDecimal.ZERO);
        assertThat(pkg.anlageS().einnahmen()).isGreaterThan(BigDecimal.ZERO);
        assertThat(pkg.anlageG().einnahmen()).isGreaterThan(BigDecimal.ZERO);

        // ── 8. ELSTER XML Export ──────────────────────────────────────
        byte[] xml = annualTaxExportService.generateElsterXml(pkg, "Max Mustermann");
        assertThat(new String(xml)).contains("Einkommensteuer");

        // ── 9. CSV Export ────────────────────────────────────────────
        byte[] csv = annualTaxExportService.generateCsv(pkg);
        assertThat(new String(csv)).contains("Anlage N;Bruttoarbeitslohn;");

        // ── 10. GoBD Audit ───────────────────────────────────────────
        List<EventLog> allAuditEvents = eventLogRepository.findAll();
        assertThat(allAuditEvents).isNotEmpty();
        assertThat(allAuditEvents).allSatisfy(e -> {
            assertThat(e.getActor()).isEqualTo("lifecycle@dreistrom.de");
            assertThat(e.getCreatedAt()).isNotNull();
            assertThat(e.getPayload()).isNotBlank();
        });

        // Verify all aggregate types have audit events
        long incomeAudits = allAuditEvents.stream()
                .filter(e -> "IncomeEntry".equals(e.getAggregateType())).count();
        long expenseAudits = allAuditEvents.stream()
                .filter(e -> "ExpenseEntry".equals(e.getAggregateType())).count();
        long invoiceAudits = allAuditEvents.stream()
                .filter(e -> "Invoice".equals(e.getAggregateType())).count();

        // At least 3 income entries (manual + invoice-generated)
        assertThat(incomeAudits).isGreaterThanOrEqualTo(3);
        assertThat(expenseAudits).isGreaterThanOrEqualTo(2);
        assertThat(invoiceAudits).isGreaterThanOrEqualTo(2);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Test data helpers
    // ═════════════════════════════════════════════════════════════════════

    private void createInvoicesAndExpenses() {
        // Freiberuf invoice
        invoiceService.create(user, InvoiceStream.FREIBERUF,
                freiberufClient.getId(), MAR_01, MAR_01.plusDays(30),
                List.of(new LineItem("IT-Beratung", new BigDecimal("40"),
                        new BigDecimal("125.00"), new BigDecimal("19"))),
                new BigDecimal("5000.00"), new BigDecimal("950.00"),
                new BigDecimal("5950.00"), VatTreatment.REGULAR, null);

        // Gewerbe invoice
        invoiceService.create(user, InvoiceStream.GEWERBE,
                gewerbeClient.getId(), JUN_01, JUN_01.plusDays(30),
                List.of(new LineItem("Warenlieferung", BigDecimal.ONE,
                        new BigDecimal("8000.00"), new BigDecimal("19"))),
                new BigDecimal("8000.00"), new BigDecimal("1520.00"),
                new BigDecimal("9520.00"), VatTreatment.REGULAR, null);

        // Shared expense
        expenseService.create(user, new BigDecimal("1000.00"), "Software",
                MAR_01, splitRule.getId(), null, "Jahreslizenz");
    }

    private void createFullDataSet() {
        // Employment income
        incomeService.create(user, IncomeStream.EMPLOYMENT,
                new BigDecimal("42000.00"), JAN_15,
                "Jahresgehalt", null, "Bruttolohn 2025");

        // Invoices (auto-create income entries for Freiberuf + Gewerbe)
        createInvoicesAndExpenses();

        // Additional direct income entries
        incomeService.create(user, IncomeStream.FREIBERUF,
                new BigDecimal("3000.00"), SEP_15,
                "Workshop", freiberufClient, "Workshop-Honorar");
    }
}
