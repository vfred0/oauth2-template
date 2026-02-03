package lt.satsyuk.service;

import lt.satsyuk.dto.ClientResponse;
import lt.satsyuk.dto.CreateClientRequest;
import lt.satsyuk.exception.ClientNotFoundException;
import lt.satsyuk.exception.PhoneAlreadyExistsException;
import lt.satsyuk.mapper.ClientMapper;
import lt.satsyuk.model.Client;
import lt.satsyuk.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    private final ClientRepository repo;
    private final ClientMapper mapper;

    public ClientService(ClientRepository repo, ClientMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Transactional
    public ClientResponse create(CreateClientRequest req) {

        if (repo.existsByPhone(req.phone())) {
            throw new PhoneAlreadyExistsException(req.phone());
        }

        Client client = mapper.toEntity(req);
        Client saved = repo.save(client);

        return mapper.toResponse(saved);
    }

    public ClientResponse get(Long id) {
        Client client = repo.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));

        return mapper.toResponse(client);
    }
}