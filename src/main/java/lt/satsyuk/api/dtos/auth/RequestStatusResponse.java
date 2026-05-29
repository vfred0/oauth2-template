package lt.satsyuk.api.dtos.auth;

import lt.satsyuk.data.entities.core.request.RequestStatus;
import lt.satsyuk.data.entities.core.request.RequestType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RequestStatusResponse(
        UUID requestId,
        RequestType type,
        RequestStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime statusChangedAt,
        Object response
) {
}

