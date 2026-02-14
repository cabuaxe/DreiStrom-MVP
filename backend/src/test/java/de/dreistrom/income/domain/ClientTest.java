package de.dreistrom.income.domain;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.income.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ClientTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AppUser user;

    @BeforeEach
    void setUp() {
        clientRepository.deleteAll();
        appUserRepository.deleteAll();
        user = appUserRepository.save(new AppUser(
                "owner@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Test Owner"));
    }

    @Test
    void persist_andRetrieve_client() {
        Client client = new Client(user, "Acme GmbH", IncomeStream.GEWERBE,
                ClientType.B2B, "DE", "DE123456789");
        Client saved = clientRepository.save(client);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Acme GmbH");
        assertThat(saved.getStreamType()).isEqualTo(IncomeStream.GEWERBE);
        assertThat(saved.getClientType()).isEqualTo(ClientType.B2B);
        assertThat(saved.getCountry()).isEqualTo("DE");
        assertThat(saved.getUstIdNr()).isEqualTo("DE123456789");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByUserIdAndActiveTrue_filtersInactive() {
        clientRepository.save(new Client(user, "Active Client", IncomeStream.FREIBERUF));
        Client inactive = new Client(user, "Inactive Client", IncomeStream.FREIBERUF,
                ClientType.B2C, "AT", null);
        clientRepository.save(inactive);

        List<Client> active = clientRepository.findByUserIdAndActiveTrue(user.getId());
        // Both are active by default
        assertThat(active).hasSize(2);
    }

    @Test
    void findByUserIdAndStreamType_filtersCorrectly() {
        clientRepository.save(new Client(user, "Freelance Client", IncomeStream.FREIBERUF));
        clientRepository.save(new Client(user, "Business Client", IncomeStream.GEWERBE));

        List<Client> freelance = clientRepository.findByUserIdAndStreamType(
                user.getId(), IncomeStream.FREIBERUF);
        assertThat(freelance).hasSize(1);
        assertThat(freelance.getFirst().getName()).isEqualTo("Freelance Client");
    }

    @Test
    void defaultValues_areApplied() {
        Client client = new Client(user, "Default Client", IncomeStream.EMPLOYMENT);
        Client saved = clientRepository.save(client);

        assertThat(saved.getCountry()).isEqualTo("DE");
        assertThat(saved.getClientType()).isEqualTo(ClientType.B2B);
        assertThat(saved.isActive()).isTrue();
    }
}
