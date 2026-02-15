package de.dreistrom.income.service;

import de.dreistrom.audit.service.AuditLogService;
import de.dreistrom.common.controller.EntityNotFoundException;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.income.domain.Client;
import de.dreistrom.income.domain.ClientType;
import de.dreistrom.income.event.ClientCreated;
import de.dreistrom.income.event.ClientModified;
import de.dreistrom.income.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public Client create(AppUser user, String name, IncomeStream streamType,
                         ClientType clientType, String country, String ustIdNr) {
        ClientType type = clientType != null ? clientType : ClientType.B2B;
        String ctry = country != null ? country : "DE";

        Client client = new Client(user, name, streamType, type, ctry, ustIdNr);
        Client saved = clientRepository.save(client);

        auditLogService.persist(new ClientCreated(saved));

        return saved;
    }

    @Transactional
    public Client update(Long clientId, Long userId, String name,
                         ClientType clientType, String country,
                         String ustIdNr, Boolean active) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client", clientId));
        if (!client.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("Client", clientId);
        }

        String beforeName = client.getName();
        String beforeClientType = client.getClientType().name();
        String beforeCountry = client.getCountry();
        String beforeUstIdNr = client.getUstIdNr();
        boolean beforeActive = client.isActive();

        client.update(name, clientType, country, ustIdNr, active);

        auditLogService.persist(new ClientModified(
                clientId,
                beforeName, client.getName(),
                beforeClientType, client.getClientType().name(),
                beforeCountry, client.getCountry(),
                beforeUstIdNr, client.getUstIdNr(),
                beforeActive, client.isActive()));

        return client;
    }

    @Transactional(readOnly = true)
    public Client getById(Long clientId, Long userId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client", clientId));
        if (!client.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("Client", clientId);
        }
        return client;
    }

    @Transactional(readOnly = true)
    public List<Client> listAll(Long userId) {
        return clientRepository.findByUserIdAndActiveTrue(userId);
    }

    @Transactional(readOnly = true)
    public List<Client> listByStreamType(Long userId, IncomeStream streamType) {
        return clientRepository.findByUserIdAndStreamType(userId, streamType);
    }

    @Transactional
    public void delete(Long clientId, Long userId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client", clientId));
        if (!client.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("Client", clientId);
        }

        String beforeName = client.getName();
        String beforeClientType = client.getClientType().name();
        String beforeCountry = client.getCountry();
        String beforeUstIdNr = client.getUstIdNr();

        client.update(client.getName(), client.getClientType(),
                client.getCountry(), client.getUstIdNr(), false);

        auditLogService.persist(new ClientModified(
                clientId,
                beforeName, client.getName(),
                beforeClientType, client.getClientType().name(),
                beforeCountry, client.getCountry(),
                beforeUstIdNr, client.getUstIdNr(),
                true, false));
    }

    @Transactional(readOnly = true)
    public boolean checkScheinselbstaendigkeitRisk(Long userId) {
        List<Client> activeFreiberufClients =
                clientRepository.findByUserIdAndStreamTypeAndActiveTrue(userId, IncomeStream.FREIBERUF);
        return activeFreiberufClients.size() <= 1;
    }
}
