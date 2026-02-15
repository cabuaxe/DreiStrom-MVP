package de.dreistrom.expense.service;

import de.dreistrom.audit.service.AuditLogService;
import de.dreistrom.common.controller.EntityNotFoundException;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.expense.domain.AllocationRule;
import de.dreistrom.expense.event.AllocationRuleCreated;
import de.dreistrom.expense.event.AllocationRuleModified;
import de.dreistrom.expense.repository.AllocationRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AllocationRuleService {

    private final AllocationRuleRepository allocationRuleRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AllocationRule create(AppUser user, String name, short freiberufPct,
                                 short gewerbePct, short personalPct) {
        AllocationRule rule = new AllocationRule(user, name, freiberufPct,
                gewerbePct, personalPct);
        AllocationRule saved = allocationRuleRepository.save(rule);

        AllocationRuleCreated event = new AllocationRuleCreated(saved);
        auditLogService.persist(event);
        eventPublisher.publishEvent(event);

        return saved;
    }

    @Transactional
    public AllocationRule update(Long ruleId, Long userId, String name,
                                 short freiberufPct, short gewerbePct, short personalPct) {
        AllocationRule rule = getOwnedRule(ruleId, userId);

        String beforeName = rule.getName();
        short beforeFreiberuf = rule.getFreiberufPct();
        short beforeGewerbe = rule.getGewerbePct();
        short beforePersonal = rule.getPersonalPct();

        rule.update(name, freiberufPct, gewerbePct, personalPct);

        AllocationRuleModified event = new AllocationRuleModified(
                ruleId, beforeName, name,
                beforeFreiberuf, freiberufPct,
                beforeGewerbe, gewerbePct,
                beforePersonal, personalPct);
        auditLogService.persist(event);
        eventPublisher.publishEvent(event);

        return rule;
    }

    @Transactional(readOnly = true)
    public AllocationRule getById(Long ruleId, Long userId) {
        return getOwnedRule(ruleId, userId);
    }

    @Transactional(readOnly = true)
    public List<AllocationRule> listAll(Long userId) {
        return allocationRuleRepository.findByUserId(userId);
    }

    @Transactional
    public void delete(Long ruleId, Long userId) {
        AllocationRule rule = getOwnedRule(ruleId, userId);
        allocationRuleRepository.delete(rule);
    }

    private AllocationRule getOwnedRule(Long ruleId, Long userId) {
        AllocationRule rule = allocationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("AllocationRule", ruleId));
        if (!rule.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("AllocationRule", ruleId);
        }
        return rule;
    }
}
