package lt.satsyuk.api.dtos.auth;

import lt.satsyuk.data.entities.core.request.RequestStatus;

import java.util.UUID;

public record RequestAcceptedResponse(
        UUID requestId,
        RequestStatus status
) {
}

