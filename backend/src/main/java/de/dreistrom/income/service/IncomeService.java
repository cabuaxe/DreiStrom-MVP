package de.dreistrom.income.service;

import de.dreistrom.audit.service.AuditLogService;
import de.dreistrom.common.controller.EntityNotFoundException;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.income.domain.Client;
import de.dreistrom.income.domain.IncomeEntry;
import de.dreistrom.income.event.IncomeEntryCreated;
import de.dreistrom.income.event.IncomeEntryModified;
import de.dreistrom.income.repository.IncomeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final IncomeEntryRepository incomeEntryRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public IncomeEntry create(AppUser user, IncomeStream streamType, BigDecimal amount,
                              LocalDate entryDate, String source, Client client,
                              String description) {
        IncomeEntry entry = new IncomeEntry(user, streamType, amount, entryDate,
                source, client, description);
        IncomeEntry saved = incomeEntryRepository.save(entry);

        IncomeEntryCreated event = new IncomeEntryCreated(saved);
        auditLogService.persist(event);
        eventPublisher.publishEvent(event);

        return saved;
    }

    @Transactional
    public IncomeEntry update(Long entryId, IncomeStream streamType, BigDecimal amount,
                              LocalDate entryDate, String source, Client client,
                              String description) {
        IncomeEntry entry = incomeEntryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("IncomeEntry", entryId));

        if (streamType != entry.getStreamType()) {
            throw new IllegalArgumentException(
                    "Cross-stream update not allowed: cannot change stream from "
                    + entry.getStreamType() + " to " + streamType);
        }

        BigDecimal beforeAmount = entry.getAmount();
        LocalDate beforeDate = entry.getEntryDate();
        String beforeSource = entry.getSource();

        entry.update(amount, entryDate, source, client, description);

        IncomeEntryModified modifiedEvent = new IncomeEntryModified(
                entryId, beforeAmount, amount, beforeDate, entryDate,
                beforeSource, source);
        auditLogService.persist(modifiedEvent);
        eventPublisher.publishEvent(modifiedEvent);

        return entry;
    }

    @Transactional(readOnly = true)
    public List<IncomeEntry> listByStream(Long userId, IncomeStream streamType) {
        return incomeEntryRepository.findByUserIdAndStreamType(userId, streamType);
    }

    @Transactional(readOnly = true)
    public List<IncomeEntry> listAll(Long userId) {
        return incomeEntryRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<IncomeEntry> listByDateRange(Long userId, LocalDate from, LocalDate to) {
        return incomeEntryRepository.findByUserIdAndEntryDateBetween(userId, from, to);
    }

    @Transactional(readOnly = true)
    public List<IncomeEntry> listByStreamAndDateRange(Long userId, IncomeStream streamType,
                                                       LocalDate from, LocalDate to) {
        return incomeEntryRepository.findByUserIdAndStreamTypeAndEntryDateBetween(
                userId, streamType, from, to);
    }

    @Transactional(readOnly = true)
    public IncomeEntry getById(Long entryId, Long userId) {
        IncomeEntry entry = incomeEntryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("IncomeEntry", entryId));
        if (!entry.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("IncomeEntry", entryId);
        }
        return entry;
    }

    @Transactional
    public void delete(Long entryId, Long userId) {
        IncomeEntry entry = incomeEntryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("IncomeEntry", entryId));
        if (!entry.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("IncomeEntry", entryId);
        }
        incomeEntryRepository.delete(entry);
    }
}
