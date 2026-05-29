package lt.satsyuk.service.core;

import lt.satsyuk.api.dtos.client.ClientResponse;
import lt.satsyuk.api.dtos.client.CreateClientRequest;
import lt.satsyuk.api.http_errors.exceptions.ClientSearchQueryTooShortException;
import lt.satsyuk.api.http_errors.exceptions.ClientNotFoundException;
import lt.satsyuk.api.http_errors.exceptions.PhoneAlreadyExistsException;
import lt.satsyuk.config.mapper.ClientMapper;
import lt.satsyuk.data.entities.core.rbac.Account;
import lt.satsyuk.data.entities.core.Client;
import lt.satsyuk.data.daos.AccountRepository;
import lt.satsyuk.data.daos.ClientRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ClientService {

    public static final int MIN_SEARCH_QUERY_LENGTH = 3;

    private final ClientRepository repo;
    private final AccountRepository accountRepository;
    private final ClientMapper mapper;
    private final int searchMaxResults;

    public ClientService(ClientRepository repo,
                         AccountRepository accountRepository,
                         ClientMapper mapper,
                         @Value("${app.clients.search.max-results:20}") int searchMaxResults) {
        this.repo = repo;
        this.accountRepository = accountRepository;
        this.mapper = mapper;
        this.searchMaxResults = Math.max(1, searchMaxResults);
    }

    @Transactional
    public ClientResponse create(CreateClientRequest req) {

        if (repo.existsByPhone(req.phone())) {
            throw new PhoneAlreadyExistsException(req.phone());
        }

        Client saved;
        try {
            Client client = mapper.toEntity(req);
            saved = repo.saveAndFlush(client);
        } catch (DataIntegrityViolationException _) {
            throw new PhoneAlreadyExistsException(req.phone());
        }

        accountRepository.saveAndFlush(Account.builder()
                .client(saved)
                .balance(BigDecimal.ZERO)
                .build());

        return mapper.toResponse(saved);
    }

    public ClientResponse get(Long id) {
        Client client = repo.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));

        return mapper.toResponse(client);
    }

    public List<ClientResponse> searchByNameOrSurname(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new ClientSearchQueryTooShortException(MIN_SEARCH_QUERY_LENGTH);
        }

        return repo.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrderByIdAsc(
                        normalizedQuery,
                        normalizedQuery,
                        PageRequest.of(0, searchMaxResults)
                ).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
