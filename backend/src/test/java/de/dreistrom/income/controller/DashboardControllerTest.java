package de.dreistrom.income.controller;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.config.RateLimitFilter;
import de.dreistrom.income.domain.IncomeEntry;
import de.dreistrom.income.repository.ClientRepository;
import de.dreistrom.income.repository.IncomeEntryRepository;
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
import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private IncomeEntryRepository incomeEntryRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    private MockHttpSession session;
    private AppUser user;

    @BeforeEach
    void setUp() throws Exception {
        rateLimitFilter.clearBuckets();
        incomeEntryRepository.deleteAll();
        clientRepository.deleteAll();
        appUserRepository.deleteAll();

        user = appUserRepository.save(new AppUser(
                "owner@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Test Owner"));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"owner@dreistrom.de\",\"password\":\"test1234\"}"))
                .andExpect(status().isOk())
                .andReturn();

        session = (MockHttpSession) loginResult.getRequest().getSession();
    }

    @Test
    void abfaerbung_noEntries_returnsZeroRatio() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/abfaerbung")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratio", is(0)))
                .andExpect(jsonPath("$.gewerbeRevenue", is(0)))
                .andExpect(jsonPath("$.selfEmployedRevenue", is(0)))
                .andExpect(jsonPath("$.thresholdExceeded", is(false)))
                .andExpect(jsonPath("$.year", is(LocalDate.now().getYear())));
    }

    @Test
    void abfaerbung_withSpecificYear_returnsYearData() throws Exception {
        createEntry(IncomeStream.GEWERBE, new BigDecimal("5000.00"), LocalDate.of(2025, 6, 1));
        createEntry(IncomeStream.FREIBERUF, new BigDecimal("10000.00"), LocalDate.of(2025, 6, 1));

        mockMvc.perform(get("/api/v1/dashboard/abfaerbung")
                        .session(session)
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year", is(2025)))
                .andExpect(jsonPath("$.gewerbeRevenue", is(5000.00)))
                .andExpect(jsonPath("$.selfEmployedRevenue", is(15000.00)))
                .andExpect(jsonPath("$.thresholdExceeded", is(false)));
    }

    @Test
    void abfaerbung_belowThreshold_returnsNotExceeded() throws Exception {
        // Gewerbe 1% of total self-employed — well below 3%
        createEntry(IncomeStream.GEWERBE, new BigDecimal("100.00"), LocalDate.of(2026, 1, 15));
        createEntry(IncomeStream.FREIBERUF, new BigDecimal("9900.00"), LocalDate.of(2026, 1, 15));

        mockMvc.perform(get("/api/v1/dashboard/abfaerbung")
                        .session(session)
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thresholdExceeded", is(false)))
                .andExpect(jsonPath("$.gewerbeRevenue", is(100.00)))
                .andExpect(jsonPath("$.selfEmployedRevenue", is(10000.00)));
    }

    @Test
    void abfaerbung_ratioExceededButAmountBelow_returnsNotExceeded() throws Exception {
        // Ratio > 3% but Gewerbe < 24500 EUR
        createEntry(IncomeStream.GEWERBE, new BigDecimal("5000.00"), LocalDate.of(2026, 3, 1));
        createEntry(IncomeStream.FREIBERUF, new BigDecimal("10000.00"), LocalDate.of(2026, 3, 1));

        mockMvc.perform(get("/api/v1/dashboard/abfaerbung")
                        .session(session)
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thresholdExceeded", is(false)))
                .andExpect(jsonPath("$.ratio", is(0.3333)));
    }

    @Test
    void abfaerbung_bothConditionsMet_returnsExceeded() throws Exception {
        // Ratio > 3% AND Gewerbe > 24500 EUR
        createEntry(IncomeStream.GEWERBE, new BigDecimal("30000.00"), LocalDate.of(2026, 3, 1));
        createEntry(IncomeStream.FREIBERUF, new BigDecimal("50000.00"), LocalDate.of(2026, 3, 1));

        mockMvc.perform(get("/api/v1/dashboard/abfaerbung")
                        .session(session)
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thresholdExceeded", is(true)))
                .andExpect(jsonPath("$.gewerbeRevenue", is(30000.00)))
                .andExpect(jsonPath("$.selfEmployedRevenue", is(80000.00)));
    }

    @Test
    void abfaerbung_onlyGewerbe_ratioIsOne() throws Exception {
        createEntry(IncomeStream.GEWERBE, new BigDecimal("30000.00"), LocalDate.of(2026, 5, 1));

        mockMvc.perform(get("/api/v1/dashboard/abfaerbung")
                        .session(session)
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratio", is(1.0)))
                .andExpect(jsonPath("$.thresholdExceeded", is(true)));
    }

    @Test
    void abfaerbung_unauthorized_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/abfaerbung"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void events_returnsSSEMediaType() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/events")
                        .session(session)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());
    }

    private IncomeEntry createEntry(IncomeStream streamType, BigDecimal amount, LocalDate date) {
        IncomeEntry entry = new IncomeEntry(user, streamType, amount, date);
        return incomeEntryRepository.save(entry);
    }
}
