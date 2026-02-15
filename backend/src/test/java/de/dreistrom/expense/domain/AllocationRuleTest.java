package de.dreistrom.expense.domain;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.expense.repository.AllocationRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AllocationRuleTest {

    @Autowired
    private AllocationRuleRepository allocationRuleRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AppUser user;

    @BeforeEach
    void setUp() {
        allocationRuleRepository.deleteAll();
        appUserRepository.deleteAll();
        user = appUserRepository.save(new AppUser(
                "expense@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Expense Tester"));
    }

    @Test
    void persist_andRetrieve_allocationRule() {
        AllocationRule rule = new AllocationRule(user, "Home Office",
                (short) 50, (short) 30, (short) 20);
        AllocationRule saved = allocationRuleRepository.save(rule);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Home Office");
        assertThat(saved.getFreiberufPct()).isEqualTo((short) 50);
        assertThat(saved.getGewerbePct()).isEqualTo((short) 30);
        assertThat(saved.getPersonalPct()).isEqualTo((short) 20);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void validation_rejectsSum_notEqualTo100() {
        AllocationRule rule = new AllocationRule(user, "Bad Rule",
                (short) 50, (short) 30, (short) 10);

        assertThatThrownBy(() -> {
            allocationRuleRepository.save(rule);
            allocationRuleRepository.flush();
        }).hasRootCauseInstanceOf(IllegalStateException.class)
          .rootCause().hasMessageContaining("must sum to 100");
    }

    @Test
    void findByUserId_filtersCorrectly() {
        AppUser otherUser = appUserRepository.save(new AppUser(
                "other@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Other User"));

        allocationRuleRepository.save(new AllocationRule(user, "Rule A",
                (short) 60, (short) 30, (short) 10));
        allocationRuleRepository.save(new AllocationRule(user, "Rule B",
                (short) 40, (short) 40, (short) 20));
        allocationRuleRepository.save(new AllocationRule(otherUser, "Rule C",
                (short) 100, (short) 0, (short) 0));

        List<AllocationRule> userRules = allocationRuleRepository.findByUserId(user.getId());
        assertThat(userRules).hasSize(2);
    }

    @Test
    void update_changesMutableFields() {
        AllocationRule rule = allocationRuleRepository.save(new AllocationRule(user, "Old Name",
                (short) 50, (short) 30, (short) 20));

        rule.update("New Name", (short) 70, (short) 20, (short) 10);
        allocationRuleRepository.flush();

        AllocationRule fetched = allocationRuleRepository.findById(rule.getId()).orElseThrow();
        assertThat(fetched.getName()).isEqualTo("New Name");
        assertThat(fetched.getFreiberufPct()).isEqualTo((short) 70);
    }
}
