package de.dreistrom.income.controller;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.config.RateLimitFilter;
import de.dreistrom.income.domain.Client;
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
class IncomeControllerTest {

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
    void create_withValidData_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/income-entries")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "streamType": "FREIBERUF",
                                    "amount": 1500.00,
                                    "entryDate": "2026-03-15",
                                    "source": "Beratung",
                                    "description": "Projektarbeit"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.streamType", is("FREIBERUF")))
                .andExpect(jsonPath("$.amount", is(1500.00)))
                .andExpect(jsonPath("$.currency", is("EUR")))
                .andExpect(jsonPath("$.entryDate", is("2026-03-15")))
                .andExpect(jsonPath("$.source", is("Beratung")))
                .andExpect(jsonPath("$.description", is("Projektarbeit")))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void create_withInvalidAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/income-entries")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "streamType": "FREIBERUF",
                                    "amount": -10.00,
                                    "entryDate": "2026-03-15"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withMissingRequired_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/income-entries")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": 100.00,
                                    "entryDate": "2026-03-15"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withNonExistentClient_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/income-entries")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "streamType": "FREIBERUF",
                                    "amount": 1500.00,
                                    "entryDate": "2026-03-15",
                                    "clientId": 99999
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_byStreamType_returnsFiltered() throws Exception {
        createEntry(IncomeStream.FREIBERUF, new BigDecimal("1000.00"), LocalDate.of(2026, 3, 1));
        createEntry(IncomeStream.GEWERBE, new BigDecimal("2000.00"), LocalDate.of(2026, 3, 15));
        createEntry(IncomeStream.FREIBERUF, new BigDecimal("3000.00"), LocalDate.of(2026, 4, 1));

        mockMvc.perform(get("/api/v1/income-entries")
                        .session(session)
                        .param("streamType", "FREIBERUF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void list_byDateRange_returnsFiltered() throws Exception {
        createEntry(IncomeStream.FREIBERUF, new BigDecimal("1000.00"), LocalDate.of(2026, 2, 15));
        createEntry(IncomeStream.GEWERBE, new BigDecimal("2000.00"), LocalDate.of(2026, 3, 15));
        createEntry(IncomeStream.FREIBERUF, new BigDecimal("3000.00"), LocalDate.of(2026, 4, 15));

        mockMvc.perform(get("/api/v1/income-entries")
                        .session(session)
                        .param("fromDate", "2026-03-01")
                        .param("toDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void list_byStreamAndDateRange_returnsCombined() throws Exception {
        createEntry(IncomeStream.FREIBERUF, new BigDecimal("1000.00"), LocalDate.of(2026, 3, 1));
        createEntry(IncomeStream.FREIBERUF, new BigDecimal("2000.00"), LocalDate.of(2026, 4, 15));
        createEntry(IncomeStream.GEWERBE, new BigDecimal("3000.00"), LocalDate.of(2026, 3, 15));

        mockMvc.perform(get("/api/v1/income-entries")
                        .session(session)
                        .param("streamType", "FREIBERUF")
                        .param("fromDate", "2026-03-01")
                        .param("toDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getById_existing_returns200() throws Exception {
        IncomeEntry entry = createEntry(
                IncomeStream.FREIBERUF, new BigDecimal("1500.00"), LocalDate.of(2026, 3, 15));

        mockMvc.perform(get("/api/v1/income-entries/{id}", entry.getId())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(entry.getId().intValue())))
                .andExpect(jsonPath("$.streamType", is("FREIBERUF")))
                .andExpect(jsonPath("$.amount", is(1500.00)));
    }

    @Test
    void getById_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/income-entries/{id}", 99999)
                        .session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_validData_returns200() throws Exception {
        IncomeEntry entry = createEntry(
                IncomeStream.FREIBERUF, new BigDecimal("1000.00"), LocalDate.of(2026, 3, 1));

        mockMvc.perform(put("/api/v1/income-entries/{id}", entry.getId())
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": 1200.00,
                                    "entryDate": "2026-03-05",
                                    "source": "Korrigiert",
                                    "description": "Nachberechnung"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(1200.00)))
                .andExpect(jsonPath("$.entryDate", is("2026-03-05")))
                .andExpect(jsonPath("$.source", is("Korrigiert")))
                .andExpect(jsonPath("$.description", is("Nachberechnung")))
                .andExpect(jsonPath("$.streamType", is("FREIBERUF")));
    }

    @Test
    void delete_existing_returns204() throws Exception {
        IncomeEntry entry = createEntry(
                IncomeStream.FREIBERUF, new BigDecimal("1000.00"), LocalDate.of(2026, 3, 1));

        mockMvc.perform(delete("/api/v1/income-entries/{id}", entry.getId())
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_nonExistent_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/income-entries/{id}", 99999)
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
    }

    private IncomeEntry createEntry(IncomeStream streamType, BigDecimal amount, LocalDate date) {
        IncomeEntry entry = new IncomeEntry(user, streamType, amount, date);
        return incomeEntryRepository.save(entry);
    }
}
