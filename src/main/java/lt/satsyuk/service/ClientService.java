package lt.satsyuk.service;

import lt.satsyuk.dto.ClientResponse;
import lt.satsyuk.dto.CreateClientRequest;
import lt.satsyuk.exception.ClientNotFoundException;
import lt.satsyuk.exception.PhoneAlreadyExistsException;
import lt.satsyuk.mapper.ClientMapper;
import lt.satsyuk.model.Account;
import lt.satsyuk.model.Client;
import lt.satsyuk.repository.AccountRepository;
import lt.satsyuk.repository.ClientRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ClientService {

    private final ClientRepository repo;
    private final AccountRepository accountRepository;
    private final ClientMapper mapper;

    public ClientService(ClientRepository repo, AccountRepository accountRepository, ClientMapper mapper) {
        this.repo = repo;
        this.accountRepository = accountRepository;
        this.mapper = mapper;
    }

    @Transactional
    public ClientResponse create(CreateClientRequest req) {

        if (repo.existsByPhone(req.phone())) {
            throw new PhoneAlreadyExistsException(req.phone());
        }

        try {
            Client client = mapper.toEntity(req);
            Client saved = repo.saveAndFlush(client);
            accountRepository.saveAndFlush(Account.builder()
                    .client(saved)
                    .balance(BigDecimal.ZERO)
                    .build());

            return mapper.toResponse(saved);
        } catch (DataIntegrityViolationException _) {
            throw new PhoneAlreadyExistsException(req.phone());
        }
    }

    public ClientResponse get(Long id) {
        Client client = repo.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));

        return mapper.toResponse(client);
    }
}
