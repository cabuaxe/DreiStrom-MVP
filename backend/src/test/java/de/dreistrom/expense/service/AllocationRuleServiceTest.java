package de.dreistrom.expense.service;

import de.dreistrom.audit.domain.EventLog;
import de.dreistrom.audit.repository.EventLogRepository;
import de.dreistrom.common.controller.EntityNotFoundException;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.expense.domain.AllocationRule;
import de.dreistrom.expense.repository.AllocationRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AllocationRuleServiceTest {

    @Autowired
    private AllocationRuleService allocationRuleService;

    @Autowired
    private AllocationRuleRepository allocationRuleRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AppUser user;

    @BeforeEach
    void setUp() {
        allocationRuleRepository.deleteAll();
        appUserRepository.deleteAll();
        eventLogRepository.deleteAll();
        user = appUserRepository.save(new AppUser(
                "allocation@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Allocation Tester"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("allocation@dreistrom.de", null, List.of()));
    }

    @Test
    void create_persistsRuleAndAuditEvent() {
        AllocationRule rule = allocationRuleService.create(user, "Home Office",
                (short) 50, (short) 30, (short) 20);

        assertThat(rule.getId()).isNotNull();
        assertThat(rule.getName()).isEqualTo("Home Office");
        assertThat(rule.getFreiberufPct()).isEqualTo((short) 50);
        assertThat(rule.getGewerbePct()).isEqualTo((short) 30);
        assertThat(rule.getPersonalPct()).isEqualTo((short) 20);

        List<EventLog> events = eventLogRepository
                .findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("AllocationRule", rule.getId());
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getEventType()).isEqualTo("ALLOCATION_RULE_CREATED");
        assertThat(events.getFirst().getPayload()).contains("Home Office");
        assertThat(events.getFirst().getActor()).isEqualTo("allocation@dreistrom.de");
    }

    @Test
    void create_invalidSum_throwsException() {
        assertThatThrownBy(() -> allocationRuleService.create(user, "Bad Rule",
                (short) 50, (short) 30, (short) 10))
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void update_modifiesFieldsAndPersistsAuditEvent() {
        AllocationRule rule = allocationRuleService.create(user, "Old Name",
                (short) 50, (short) 30, (short) 20);

        AllocationRule updated = allocationRuleService.update(rule.getId(), user.getId(),
                "New Name", (short) 70, (short) 20, (short) 10);

        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getFreiberufPct()).isEqualTo((short) 70);

        List<EventLog> events = eventLogRepository
                .findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("AllocationRule", rule.getId());
        assertThat(events).hasSize(2);
        assertThat(events.get(1).getEventType()).isEqualTo("ALLOCATION_RULE_MODIFIED");
        assertThat(events.get(1).getPayload()).contains("\"before\"");
        assertThat(events.get(1).getPayload()).contains("\"after\"");
    }

    @Test
    void update_nonExistentRule_throwsEntityNotFound() {
        assertThatThrownBy(() -> allocationRuleService.update(999L, user.getId(),
                "Test", (short) 100, (short) 0, (short) 0))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("AllocationRule");
    }

    @Test
    void update_otherUsersRule_throwsEntityNotFound() {
        AppUser otherUser = appUserRepository.save(new AppUser(
                "other@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Other User"));
        AllocationRule rule = allocationRuleService.create(otherUser, "Other Rule",
                (short) 100, (short) 0, (short) 0);

        assertThatThrownBy(() -> allocationRuleService.update(rule.getId(), user.getId(),
                "Hijack", (short) 100, (short) 0, (short) 0))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void listAll_returnsOnlyUserRules() {
        AppUser otherUser = appUserRepository.save(new AppUser(
                "other@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Other User"));

        allocationRuleService.create(user, "Rule A", (short) 60, (short) 30, (short) 10);
        allocationRuleService.create(user, "Rule B", (short) 40, (short) 40, (short) 20);
        allocationRuleService.create(otherUser, "Rule C", (short) 100, (short) 0, (short) 0);

        List<AllocationRule> rules = allocationRuleService.listAll(user.getId());
        assertThat(rules).hasSize(2);
    }

    @Test
    void delete_removesRule() {
        AllocationRule rule = allocationRuleService.create(user, "To Delete",
                (short) 100, (short) 0, (short) 0);

        allocationRuleService.delete(rule.getId(), user.getId());

        assertThat(allocationRuleRepository.findById(rule.getId())).isEmpty();
    }

    @Test
    void delete_otherUsersRule_throwsEntityNotFound() {
        AppUser otherUser = appUserRepository.save(new AppUser(
                "other@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Other User"));
        AllocationRule rule = allocationRuleService.create(otherUser, "Other Rule",
                (short) 100, (short) 0, (short) 0);

        assertThatThrownBy(() -> allocationRuleService.delete(rule.getId(), user.getId()))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
