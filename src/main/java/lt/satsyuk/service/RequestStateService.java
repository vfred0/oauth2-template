package lt.satsyuk.service;

import lt.satsyuk.exception.RequestNotFoundException;
import lt.satsyuk.model.Request;
import lt.satsyuk.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestStateService {

    private final RequestRepository requestRepository;

    @Transactional(readOnly = true)
    public Request getRequired(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markInProgress(UUID requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
        request.markInProgress(now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessed(UUID requestId, String responseData) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
        request.markProcessed(responseData, now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessingError(UUID requestId, String responseData) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
        request.markProcessingError(responseData, now());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}


