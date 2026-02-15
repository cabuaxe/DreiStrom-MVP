package de.dreistrom.expense.controller;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.config.RateLimitFilter;
import de.dreistrom.expense.domain.AllocationRule;
import de.dreistrom.expense.repository.AllocationRuleRepository;
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
class AllocationRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AllocationRuleRepository allocationRuleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    private MockHttpSession session;
    private AppUser user;

    @BeforeEach
    void setUp() throws Exception {
        rateLimitFilter.clearBuckets();
        allocationRuleRepository.deleteAll();
        appUserRepository.deleteAll();

        user = appUserRepository.save(new AppUser(
                "rule-test@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Rule Tester"));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rule-test@dreistrom.de\",\"password\":\"test1234\"}"))
                .andExpect(status().isOk())
                .andReturn();

        session = (MockHttpSession) loginResult.getRequest().getSession();
    }

    @Test
    void create_validRule_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/allocation-rules")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Büro 50/30/20",
                                    "freiberufPct": 50,
                                    "gewerbePct": 30,
                                    "personalPct": 20
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name", is("Büro 50/30/20")))
                .andExpect(jsonPath("$.freiberufPct", is(50)))
                .andExpect(jsonPath("$.gewerbePct", is(30)))
                .andExpect(jsonPath("$.personalPct", is(20)))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void create_invalidSum_returns400() throws Exception {
        // Percentages don't sum to 100 — controller validation rejects
        mockMvc.perform(post("/api/v1/allocation-rules")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Invalid",
                                    "freiberufPct": 50,
                                    "gewerbePct": 30,
                                    "personalPct": 30
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/allocation-rules")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "freiberufPct": 50,
                                    "gewerbePct": 30,
                                    "personalPct": 20
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returnsAll() throws Exception {
        allocationRuleRepository.save(
                new AllocationRule(user, "Rule A", (short) 40, (short) 40, (short) 20));
        allocationRuleRepository.save(
                new AllocationRule(user, "Rule B", (short) 60, (short) 30, (short) 10));

        mockMvc.perform(get("/api/v1/allocation-rules")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getById_existing_returns200() throws Exception {
        AllocationRule rule = allocationRuleRepository.save(
                new AllocationRule(user, "Büro 50/30/20", (short) 50, (short) 30, (short) 20));

        mockMvc.perform(get("/api/v1/allocation-rules/{id}", rule.getId())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(rule.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Büro 50/30/20")))
                .andExpect(jsonPath("$.freiberufPct", is(50)));
    }

    @Test
    void getById_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/allocation-rules/{id}", 99999)
                        .session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_validData_returns200() throws Exception {
        AllocationRule rule = allocationRuleRepository.save(
                new AllocationRule(user, "Original", (short) 50, (short) 30, (short) 20));

        mockMvc.perform(put("/api/v1/allocation-rules/{id}", rule.getId())
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Updated",
                                    "freiberufPct": 40,
                                    "gewerbePct": 40,
                                    "personalPct": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated")))
                .andExpect(jsonPath("$.freiberufPct", is(40)))
                .andExpect(jsonPath("$.gewerbePct", is(40)))
                .andExpect(jsonPath("$.personalPct", is(20)));
    }

    @Test
    void update_invalidSum_returns400() throws Exception {
        AllocationRule rule = allocationRuleRepository.save(
                new AllocationRule(user, "Original", (short) 50, (short) 30, (short) 20));

        mockMvc.perform(put("/api/v1/allocation-rules/{id}", rule.getId())
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Invalid",
                                    "freiberufPct": 10,
                                    "gewerbePct": 10,
                                    "personalPct": 10
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_existing_returns204() throws Exception {
        AllocationRule rule = allocationRuleRepository.save(
                new AllocationRule(user, "To Delete", (short) 50, (short) 30, (short) 20));

        mockMvc.perform(delete("/api/v1/allocation-rules/{id}", rule.getId())
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_nonExistent_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/allocation-rules/{id}", 99999)
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
    }
}
