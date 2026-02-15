package de.dreistrom.invoicing.controller;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.config.RateLimitFilter;
import de.dreistrom.income.domain.Client;
import de.dreistrom.income.domain.ClientType;
import de.dreistrom.income.repository.ClientRepository;
import de.dreistrom.invoicing.domain.Invoice;
import de.dreistrom.invoicing.domain.InvoiceStatus;
import de.dreistrom.invoicing.domain.InvoiceStream;
import de.dreistrom.invoicing.domain.VatTreatment;
import de.dreistrom.invoicing.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    private MockHttpSession session;
    private AppUser user;
    private Client client;

    @BeforeEach
    void setUp() throws Exception {
        rateLimitFilter.clearBuckets();
        invoiceRepository.deleteAll();
        clientRepository.deleteAll();
        appUserRepository.deleteAll();

        user = appUserRepository.save(new AppUser(
                "invoice-test@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Invoice Tester"));

        client = clientRepository.save(new Client(
                user, "Musterfirma GmbH", IncomeStream.FREIBERUF));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invoice-test@dreistrom.de\",\"password\":\"test1234\"}"))
                .andExpect(status().isOk())
                .andReturn();

        session = (MockHttpSession) loginResult.getRequest().getSession();
    }

    @Nested
    class CreateInvoice {

        @Test
        void create_withValidData_returns201() throws Exception {
            mockMvc.perform(post("/api/v1/invoices")
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCreateJson()))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.streamType", is("FREIBERUF")))
                    .andExpect(jsonPath("$.number").isString())
                    .andExpect(jsonPath("$.client.id", is(client.getId().intValue())))
                    .andExpect(jsonPath("$.client.name", is("Musterfirma GmbH")))
                    .andExpect(jsonPath("$.invoiceDate", is("2026-03-01")))
                    .andExpect(jsonPath("$.dueDate", is("2026-03-31")))
                    .andExpect(jsonPath("$.lineItems", hasSize(1)))
                    .andExpect(jsonPath("$.netTotal", is(1000.00)))
                    .andExpect(jsonPath("$.vat", is(190.00)))
                    .andExpect(jsonPath("$.grossTotal", is(1190.00)))
                    .andExpect(jsonPath("$.currency", is("EUR")))
                    .andExpect(jsonPath("$.status", is("DRAFT")))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty())
                    .andExpect(jsonPath("$.updatedAt").isNotEmpty());
        }

        @Test
        void create_withMultipleLineItems_returns201() throws Exception {
            mockMvc.perform(post("/api/v1/invoices")
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "streamType": "FREIBERUF",
                                        "clientId": %d,
                                        "invoiceDate": "2026-03-01",
                                        "dueDate": "2026-03-31",
                                        "lineItems": [
                                            {"description": "Beratung", "quantity": 10, "unitPrice": 100.00, "vatRate": 19},
                                            {"description": "Fahrtkosten", "quantity": 1, "unitPrice": 50.00, "vatRate": 19}
                                        ],
                                        "netTotal": 1050.00,
                                        "vat": 199.50,
                                        "grossTotal": 1249.50
                                    }
                                    """.formatted(client.getId())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.lineItems", hasSize(2)));
        }

        @Test
        void create_withMissingStreamType_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/invoices")
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "clientId": %d,
                                        "invoiceDate": "2026-03-01",
                                        "lineItems": [{"description": "Beratung", "quantity": 1, "unitPrice": 100, "vatRate": 19}],
                                        "netTotal": 100.00,
                                        "vat": 19.00,
                                        "grossTotal": 119.00
                                    }
                                    """.formatted(client.getId())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void create_withEmptyLineItems_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/invoices")
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "streamType": "FREIBERUF",
                                        "clientId": %d,
                                        "invoiceDate": "2026-03-01",
                                        "lineItems": [],
                                        "netTotal": 0.00,
                                        "vat": 0.00,
                                        "grossTotal": 0.00
                                    }
                                    """.formatted(client.getId())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void create_withNonExistentClient_returns404() throws Exception {
            mockMvc.perform(post("/api/v1/invoices")
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "streamType": "FREIBERUF",
                                        "clientId": 99999,
                                        "invoiceDate": "2026-03-01",
                                        "lineItems": [{"description": "Beratung", "quantity": 1, "unitPrice": 100, "vatRate": 19}],
                                        "netTotal": 100.00,
                                        "vat": 19.00,
                                        "grossTotal": 119.00
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        void create_kleinunternehmer_withNotice_returns201() throws Exception {
            mockMvc.perform(post("/api/v1/invoices")
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "streamType": "FREIBERUF",
                                        "clientId": %d,
                                        "invoiceDate": "2026-03-01",
                                        "lineItems": [{"description": "Beratung", "quantity": 5, "unitPrice": 100.00, "vatRate": 0}],
                                        "netTotal": 500.00,
                                        "vat": 0.00,
                                        "grossTotal": 500.00,
                                        "vatTreatment": "SMALL_BUSINESS",
                                        "notes": "Gemäß §19 UStG wird keine Umsatzsteuer berechnet."
                                    }
                                    """.formatted(client.getId())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.vatTreatment", is("SMALL_BUSINESS")))
                    .andExpect(jsonPath("$.vat", is(0.0)));
        }

        @Test
        void create_kleinunternehmer_withoutNotice_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/invoices")
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "streamType": "FREIBERUF",
                                        "clientId": %d,
                                        "invoiceDate": "2026-03-01",
                                        "lineItems": [{"description": "Beratung", "quantity": 5, "unitPrice": 100.00, "vatRate": 0}],
                                        "netTotal": 500.00,
                                        "vat": 0.00,
                                        "grossTotal": 500.00,
                                        "vatTreatment": "SMALL_BUSINESS"
                                    }
                                    """.formatted(client.getId())))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class GetInvoice {

        @Test
        void getById_existing_returns200() throws Exception {
            Long invoiceId = createInvoiceAndReturnId();

            mockMvc.perform(get("/api/v1/invoices/{id}", invoiceId)
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(invoiceId.intValue())))
                    .andExpect(jsonPath("$.streamType", is("FREIBERUF")))
                    .andExpect(jsonPath("$.client.name", is("Musterfirma GmbH")));
        }

        @Test
        void getById_nonExistent_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/invoices/{id}", 99999)
                            .session(session))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class ListInvoices {

        @Test
        void list_all_returnsInvoices() throws Exception {
            createInvoiceAndReturnId();
            createInvoiceAndReturnId();

            mockMvc.perform(get("/api/v1/invoices")
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void list_byStreamType_returnsFiltered() throws Exception {
            createInvoiceAndReturnId();

            Client gewerbeClient = clientRepository.save(new Client(
                    user, "Gewerbe GmbH", IncomeStream.GEWERBE));
            createInvoiceWithStream(gewerbeClient, InvoiceStream.GEWERBE);

            mockMvc.perform(get("/api/v1/invoices")
                            .session(session)
                            .param("streamType", "FREIBERUF"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].streamType", is("FREIBERUF")));
        }

        @Test
        void list_byStatus_returnsFiltered() throws Exception {
            createInvoiceAndReturnId();
            createInvoiceAndReturnId();

            mockMvc.perform(get("/api/v1/invoices")
                            .session(session)
                            .param("status", "DRAFT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void list_byDateRange_returnsFiltered() throws Exception {
            createInvoiceAndReturnId();

            mockMvc.perform(get("/api/v1/invoices")
                            .session(session)
                            .param("fromDate", "2026-03-01")
                            .param("toDate", "2026-03-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void list_byClient_returnsFiltered() throws Exception {
            createInvoiceAndReturnId();

            Client otherClient = clientRepository.save(new Client(
                    user, "Andere Firma", IncomeStream.FREIBERUF));
            createInvoiceWithClient(otherClient);

            mockMvc.perform(get("/api/v1/invoices")
                            .session(session)
                            .param("clientId", String.valueOf(client.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].client.name", is("Musterfirma GmbH")));
        }

        @Test
        void list_empty_returnsEmptyArray() throws Exception {
            mockMvc.perform(get("/api/v1/invoices")
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    class UpdateInvoice {

        @Test
        void update_draftInvoice_returns200() throws Exception {
            Long invoiceId = createInvoiceAndReturnId();

            mockMvc.perform(put("/api/v1/invoices/{id}", invoiceId)
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "clientId": %d,
                                        "invoiceDate": "2026-03-15",
                                        "dueDate": "2026-04-15",
                                        "lineItems": [{"description": "Beratung (korrigiert)", "quantity": 8, "unitPrice": 120.00, "vatRate": 19}],
                                        "netTotal": 960.00,
                                        "vat": 182.40,
                                        "grossTotal": 1142.40
                                    }
                                    """.formatted(client.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.invoiceDate", is("2026-03-15")))
                    .andExpect(jsonPath("$.netTotal", is(960.00)))
                    .andExpect(jsonPath("$.grossTotal", is(1142.40)));
        }

        @Test
        void update_sentInvoice_returns400() throws Exception {
            Long invoiceId = createInvoiceAndReturnId();

            // Transition to SENT
            mockMvc.perform(patch("/api/v1/invoices/{id}/status", invoiceId)
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"SENT\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(put("/api/v1/invoices/{id}", invoiceId)
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "clientId": %d,
                                        "invoiceDate": "2026-03-15",
                                        "lineItems": [{"description": "Test", "quantity": 1, "unitPrice": 100, "vatRate": 19}],
                                        "netTotal": 100.00,
                                        "vat": 19.00,
                                        "grossTotal": 119.00
                                    }
                                    """.formatted(client.getId())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void update_nonExistent_returns404() throws Exception {
            mockMvc.perform(put("/api/v1/invoices/{id}", 99999)
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "clientId": %d,
                                        "invoiceDate": "2026-03-15",
                                        "lineItems": [{"description": "Test", "quantity": 1, "unitPrice": 100, "vatRate": 19}],
                                        "netTotal": 100.00,
                                        "vat": 19.00,
                                        "grossTotal": 119.00
                                    }
                                    """.formatted(client.getId())))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class StatusTransitions {

        @Test
        void updateStatus_draftToSent_returns200() throws Exception {
            Long invoiceId = createInvoiceAndReturnId();

            mockMvc.perform(patch("/api/v1/invoices/{id}/status", invoiceId)
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"SENT\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("SENT")));
        }

        @Test
        void updateStatus_sentToPaid_returns200() throws Exception {
            Long invoiceId = createInvoiceAndReturnId();

            // DRAFT → SENT
            mockMvc.perform(patch("/api/v1/invoices/{id}/status", invoiceId)
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"SENT\"}"))
                    .andExpect(status().isOk());

            // SENT → PAID
            mockMvc.perform(patch("/api/v1/invoices/{id}/status", invoiceId)
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"PAID\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PAID")));
        }

        @Test
        void updateStatus_draftToCancelled_returns200() throws Exception {
            Long invoiceId = createInvoiceAndReturnId();

            mockMvc.perform(patch("/api/v1/invoices/{id}/status", invoiceId)
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"CANCELLED\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CANCELLED")));
        }

        @Test
        void updateStatus_draftToPaid_returns400() throws Exception {
            Long invoiceId = createInvoiceAndReturnId();

            mockMvc.perform(patch("/api/v1/invoices/{id}/status", invoiceId)
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"PAID\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void updateStatus_paidToAnything_returns400() throws Exception {
            Long invoiceId = createInvoiceAndReturnId();

            // DRAFT → SENT → PAID
            mockMvc.perform(patch("/api/v1/invoices/{id}/status", invoiceId)
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"SENT\"}"));
            mockMvc.perform(patch("/api/v1/invoices/{id}/status", invoiceId)
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"PAID\"}"));

            // PAID → CANCELLED should fail
            mockMvc.perform(patch("/api/v1/invoices/{id}/status", invoiceId)
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"CANCELLED\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void updateStatus_nonExistent_returns404() throws Exception {
            mockMvc.perform(patch("/api/v1/invoices/{id}/status", 99999)
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"SENT\"}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DeleteInvoice {

        @Test
        void delete_draftInvoice_returns204() throws Exception {
            Long invoiceId = createInvoiceAndReturnId();

            mockMvc.perform(delete("/api/v1/invoices/{id}", invoiceId)
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf()))
                    .andExpect(status().isNoContent());

            // Verify it's gone
            mockMvc.perform(get("/api/v1/invoices/{id}", invoiceId)
                            .session(session))
                    .andExpect(status().isNotFound());
        }

        @Test
        void delete_sentInvoice_returns400() throws Exception {
            Long invoiceId = createInvoiceAndReturnId();

            // Transition to SENT
            mockMvc.perform(patch("/api/v1/invoices/{id}/status", invoiceId)
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"SENT\"}"));

            mockMvc.perform(delete("/api/v1/invoices/{id}", invoiceId)
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void delete_nonExistent_returns404() throws Exception {
            mockMvc.perform(delete("/api/v1/invoices/{id}", 99999)
                            .session(session)
                            .with(SecurityMockMvcRequestPostProcessors.csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class PdfGeneration {

        @Test
        void generatePdf_existingInvoice_returnsPdf() throws Exception {
            Long invoiceId = createInvoiceAndReturnId();

            mockMvc.perform(get("/api/v1/invoices/{id}/pdf", invoiceId)
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string("Content-Disposition",
                            startsWith("attachment; filename=\"")));
        }

        @Test
        void generatePdf_nonExistent_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/invoices/{id}/pdf", 99999)
                            .session(session))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Authentication {

        @Test
        void create_unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/api/v1/invoices")
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCreateJson()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void list_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/invoices"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private String validCreateJson() {
        return """
                {
                    "streamType": "FREIBERUF",
                    "clientId": %d,
                    "invoiceDate": "2026-03-01",
                    "dueDate": "2026-03-31",
                    "lineItems": [{"description": "IT-Beratung", "quantity": 10, "unitPrice": 100.00, "vatRate": 19}],
                    "netTotal": 1000.00,
                    "vat": 190.00,
                    "grossTotal": 1190.00
                }
                """.formatted(client.getId());
    }

    private Long createInvoiceAndReturnId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/invoices")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isCreated())
                .andReturn();

        String locationHeader = result.getResponse().getHeader("Location");
        return Long.parseLong(locationHeader.substring(locationHeader.lastIndexOf('/') + 1));
    }

    private void createInvoiceWithStream(Client streamClient, InvoiceStream stream) throws Exception {
        mockMvc.perform(post("/api/v1/invoices")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "streamType": "%s",
                                    "clientId": %d,
                                    "invoiceDate": "2026-03-01",
                                    "lineItems": [{"description": "Service", "quantity": 1, "unitPrice": 500.00, "vatRate": 19}],
                                    "netTotal": 500.00,
                                    "vat": 95.00,
                                    "grossTotal": 595.00
                                }
                                """.formatted(stream.name(), streamClient.getId())))
                .andExpect(status().isCreated());
    }

    private void createInvoiceWithClient(Client targetClient) throws Exception {
        mockMvc.perform(post("/api/v1/invoices")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "streamType": "FREIBERUF",
                                    "clientId": %d,
                                    "invoiceDate": "2026-03-01",
                                    "lineItems": [{"description": "Service", "quantity": 1, "unitPrice": 500.00, "vatRate": 19}],
                                    "netTotal": 500.00,
                                    "vat": 95.00,
                                    "grossTotal": 595.00
                                }
                                """.formatted(targetClient.getId())))
                .andExpect(status().isCreated());
    }
}
