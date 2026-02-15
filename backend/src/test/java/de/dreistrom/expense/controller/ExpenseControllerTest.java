package de.dreistrom.expense.controller;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.config.RateLimitFilter;
import de.dreistrom.expense.domain.AllocationRule;
import de.dreistrom.expense.domain.DepreciationAsset;
import de.dreistrom.expense.domain.ExpenseEntry;
import de.dreistrom.expense.repository.AllocationRuleRepository;
import de.dreistrom.expense.repository.DepreciationAssetRepository;
import de.dreistrom.expense.repository.ExpenseEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ExpenseEntryRepository expenseEntryRepository;

    @Autowired
    private AllocationRuleRepository allocationRuleRepository;

    @Autowired
    private DepreciationAssetRepository depreciationAssetRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    private MockHttpSession session;
    private AppUser user;

    @BeforeEach
    void setUp() throws Exception {
        rateLimitFilter.clearBuckets();
        depreciationAssetRepository.deleteAll();
        expenseEntryRepository.deleteAll();
        allocationRuleRepository.deleteAll();
        appUserRepository.deleteAll();

        user = appUserRepository.save(new AppUser(
                "expense-test@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Expense Tester"));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"expense-test@dreistrom.de\",\"password\":\"test1234\"}"))
                .andExpect(status().isOk())
                .andReturn();

        session = (MockHttpSession) loginResult.getRequest().getSession();
    }

    @Test
    void create_withValidData_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/expenses")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": 49.90,
                                    "category": "Büromaterial",
                                    "entryDate": "2026-03-10",
                                    "description": "Druckerpapier"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.amount", is(49.90)))
                .andExpect(jsonPath("$.currency", is("EUR")))
                .andExpect(jsonPath("$.category", is("Büromaterial")))
                .andExpect(jsonPath("$.entryDate", is("2026-03-10")))
                .andExpect(jsonPath("$.description", is("Druckerpapier")))
                .andExpect(jsonPath("$.gwg", is(true)))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void create_withAllocationRule_returns201() throws Exception {
        AllocationRule rule = allocationRuleRepository.save(
                new AllocationRule(user, "Büro 60/30/10", (short) 60, (short) 30, (short) 10));

        mockMvc.perform(post("/api/v1/expenses")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": 250.00,
                                    "category": "Telefon",
                                    "entryDate": "2026-03-15",
                                    "allocationRuleId": %d
                                }
                                """.formatted(rule.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allocationRule.id", is(rule.getId().intValue())))
                .andExpect(jsonPath("$.allocationRule.name", is("Büro 60/30/10")))
                .andExpect(jsonPath("$.allocationRule.freiberufPct", is(60)))
                .andExpect(jsonPath("$.allocationRule.gewerbePct", is(30)))
                .andExpect(jsonPath("$.allocationRule.personalPct", is(10)));
    }

    @Test
    void create_withInvalidAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/expenses")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": -5.00,
                                    "category": "Test",
                                    "entryDate": "2026-03-10"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withMissingRequired_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/expenses")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": 100.00
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withNonExistentAllocationRule_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/expenses")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": 100.00,
                                    "category": "Test",
                                    "entryDate": "2026-03-10",
                                    "allocationRuleId": 99999
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_all_returnsEntries() throws Exception {
        createEntry(new BigDecimal("100.00"), "Büromaterial", LocalDate.of(2026, 3, 1));
        createEntry(new BigDecimal("200.00"), "Telefon", LocalDate.of(2026, 3, 15));

        mockMvc.perform(get("/api/v1/expenses")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void list_byCategory_returnsFiltered() throws Exception {
        createEntry(new BigDecimal("100.00"), "Büromaterial", LocalDate.of(2026, 3, 1));
        createEntry(new BigDecimal("200.00"), "Telefon", LocalDate.of(2026, 3, 15));
        createEntry(new BigDecimal("300.00"), "Büromaterial", LocalDate.of(2026, 4, 1));

        mockMvc.perform(get("/api/v1/expenses")
                        .session(session)
                        .param("category", "Büromaterial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void list_byDateRange_returnsFiltered() throws Exception {
        createEntry(new BigDecimal("100.00"), "Büromaterial", LocalDate.of(2026, 2, 15));
        createEntry(new BigDecimal("200.00"), "Telefon", LocalDate.of(2026, 3, 15));
        createEntry(new BigDecimal("300.00"), "Büromaterial", LocalDate.of(2026, 4, 15));

        mockMvc.perform(get("/api/v1/expenses")
                        .session(session)
                        .param("fromDate", "2026-03-01")
                        .param("toDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void list_byCategoryAndDateRange_returnsCombined() throws Exception {
        createEntry(new BigDecimal("100.00"), "Büromaterial", LocalDate.of(2026, 3, 1));
        createEntry(new BigDecimal("200.00"), "Büromaterial", LocalDate.of(2026, 4, 15));
        createEntry(new BigDecimal("300.00"), "Telefon", LocalDate.of(2026, 3, 15));

        mockMvc.perform(get("/api/v1/expenses")
                        .session(session)
                        .param("category", "Büromaterial")
                        .param("fromDate", "2026-03-01")
                        .param("toDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getById_existing_returns200() throws Exception {
        ExpenseEntry entry = createEntry(
                new BigDecimal("150.00"), "Büromaterial", LocalDate.of(2026, 3, 10));

        mockMvc.perform(get("/api/v1/expenses/{id}", entry.getId())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(entry.getId().intValue())))
                .andExpect(jsonPath("$.amount", is(150.00)))
                .andExpect(jsonPath("$.category", is("Büromaterial")));
    }

    @Test
    void getById_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/expenses/{id}", 99999)
                        .session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_validData_returns200() throws Exception {
        ExpenseEntry entry = createEntry(
                new BigDecimal("100.00"), "Büromaterial", LocalDate.of(2026, 3, 1));

        mockMvc.perform(put("/api/v1/expenses/{id}", entry.getId())
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": 120.00,
                                    "category": "Bürobedarf",
                                    "entryDate": "2026-03-05",
                                    "description": "Korrigiert"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(120.00)))
                .andExpect(jsonPath("$.category", is("Bürobedarf")))
                .andExpect(jsonPath("$.entryDate", is("2026-03-05")))
                .andExpect(jsonPath("$.description", is("Korrigiert")));
    }

    @Test
    void delete_existing_returns204() throws Exception {
        ExpenseEntry entry = createEntry(
                new BigDecimal("100.00"), "Büromaterial", LocalDate.of(2026, 3, 1));

        mockMvc.perform(delete("/api/v1/expenses/{id}", entry.getId())
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_nonExistent_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/expenses/{id}", 99999)
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDepreciationSchedule_withLinkedAsset_returnsSchedule() throws Exception {
        ExpenseEntry entry = createEntry(
                new BigDecimal("1200.00"), "Computer", LocalDate.of(2026, 1, 15));

        BigDecimal annualAfa = new BigDecimal("1200.00").divide(
                new BigDecimal("36").divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP),
                2, RoundingMode.HALF_UP);

        DepreciationAsset asset = depreciationAssetRepository.save(
                new DepreciationAsset(user, "Computer", LocalDate.of(2026, 1, 15),
                        new BigDecimal("1200.00"), 36, annualAfa, entry));

        mockMvc.perform(get("/api/v1/expenses/{id}/depreciation", entry.getId())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].year", is(2026)))
                .andExpect(jsonPath("$[0].depreciation").isNumber())
                .andExpect(jsonPath("$[0].remainingBookValue").isNumber());
    }

    @Test
    void getDepreciationSchedule_withoutLinkedAsset_returns404() throws Exception {
        ExpenseEntry entry = createEntry(
                new BigDecimal("50.00"), "Büromaterial", LocalDate.of(2026, 3, 1));

        mockMvc.perform(get("/api/v1/expenses/{id}/depreciation", entry.getId())
                        .session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_gwgFlag_trueForSmallAmount() throws Exception {
        mockMvc.perform(post("/api/v1/expenses")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": 800.00,
                                    "category": "Werkzeug",
                                    "entryDate": "2026-03-10"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gwg", is(true)));
    }

    @Test
    void create_gwgFlag_falseForLargeAmount() throws Exception {
        mockMvc.perform(post("/api/v1/expenses")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": 801.00,
                                    "category": "Laptop",
                                    "entryDate": "2026-03-10"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gwg", is(false)));
    }

    private ExpenseEntry createEntry(BigDecimal amount, String category, LocalDate date) {
        ExpenseEntry entry = new ExpenseEntry(user, amount, category, date);
        return expenseEntryRepository.save(entry);
    }
}
