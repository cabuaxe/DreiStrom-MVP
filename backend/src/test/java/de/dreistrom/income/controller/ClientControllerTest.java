package de.dreistrom.income.controller;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.config.RateLimitFilter;
import de.dreistrom.income.domain.Client;
import de.dreistrom.income.domain.ClientType;
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
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private IncomeEntryRepository incomeEntryRepository;

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
        mockMvc.perform(post("/api/v1/clients")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Acme GmbH",
                                    "streamType": "FREIBERUF"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name", is("Acme GmbH")))
                .andExpect(jsonPath("$.streamType", is("FREIBERUF")))
                .andExpect(jsonPath("$.clientType", is("B2B")))
                .andExpect(jsonPath("$.country", is("DE")))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void create_withFullData_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "BigCorp AG",
                                    "streamType": "GEWERBE",
                                    "clientType": "B2C",
                                    "country": "AT",
                                    "ustIdNr": "ATU12345678"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name", is("BigCorp AG")))
                .andExpect(jsonPath("$.streamType", is("GEWERBE")))
                .andExpect(jsonPath("$.clientType", is("B2C")))
                .andExpect(jsonPath("$.country", is("AT")))
                .andExpect(jsonPath("$.ustIdNr", is("ATU12345678")));
    }

    @Test
    void create_withMissingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "streamType": "FREIBERUF"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withMissingStreamType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Acme GmbH"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returnsAllActiveClients() throws Exception {
        createClient("Client A", IncomeStream.FREIBERUF);
        createClient("Client B", IncomeStream.GEWERBE);

        mockMvc.perform(get("/api/v1/clients")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients", hasSize(2)));
    }

    @Test
    void list_byStreamType_returnsFiltered() throws Exception {
        createClient("Client A", IncomeStream.FREIBERUF);
        createClient("Client B", IncomeStream.GEWERBE);
        createClient("Client C", IncomeStream.FREIBERUF);

        mockMvc.perform(get("/api/v1/clients")
                        .session(session)
                        .param("streamType", "FREIBERUF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients", hasSize(2)));
    }

    @Test
    void list_withScheinselbstaendigkeitWarning() throws Exception {
        createClient("Only Client", IncomeStream.FREIBERUF);

        mockMvc.perform(get("/api/v1/clients")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheinselbstaendigkeitWarning", is(true)));
    }

    @Test
    void list_withoutScheinselbstaendigkeitWarning() throws Exception {
        createClient("Client A", IncomeStream.FREIBERUF);
        createClient("Client B", IncomeStream.FREIBERUF);

        mockMvc.perform(get("/api/v1/clients")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheinselbstaendigkeitWarning", is(false)));
    }

    @Test
    void getById_existing_returns200() throws Exception {
        Client client = createClient("Acme GmbH", IncomeStream.FREIBERUF);

        mockMvc.perform(get("/api/v1/clients/{id}", client.getId())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(client.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Acme GmbH")))
                .andExpect(jsonPath("$.streamType", is("FREIBERUF")));
    }

    @Test
    void getById_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{id}", 99999)
                        .session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_validData_returns200() throws Exception {
        Client client = createClient("Acme GmbH", IncomeStream.FREIBERUF);

        mockMvc.perform(put("/api/v1/clients/{id}", client.getId())
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Acme AG",
                                    "clientType": "B2C",
                                    "country": "AT",
                                    "ustIdNr": "ATU99999999",
                                    "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Acme AG")))
                .andExpect(jsonPath("$.clientType", is("B2C")))
                .andExpect(jsonPath("$.country", is("AT")))
                .andExpect(jsonPath("$.ustIdNr", is("ATU99999999")))
                .andExpect(jsonPath("$.streamType", is("FREIBERUF")));
    }

    @Test
    void delete_existing_returns204() throws Exception {
        Client client = createClient("Acme GmbH", IncomeStream.FREIBERUF);

        mockMvc.perform(delete("/api/v1/clients/{id}", client.getId())
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_nonExistent_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/{id}", 99999)
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
    }

    private Client createClient(String name, IncomeStream streamType) {
        Client client = new Client(user, name, streamType,
                ClientType.B2B, "DE", null);
        return clientRepository.save(client);
    }
}
