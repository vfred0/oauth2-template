package lt.satsyuk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lt.satsyuk.dto.ApiKeyResponse;
import lt.satsyuk.dto.AppResponse;
import lt.satsyuk.dto.CreateApiKeyRequest;
import lt.satsyuk.dto.IssuedApiKeyResponse;
import lt.satsyuk.service.ApiKeyManagementService;
import lt.satsyuk.service.CreateApiKeyCommand;
import lt.satsyuk.service.IssuedApiKey;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/api-keys")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('apikeys:manage')")
@Tag(name = "API Keys", description = "API key management")
public class ApiKeyController {

    private final ApiKeyManagementService managementService;

    @PostMapping
    @Operation(summary = "Issue API key",
            description = "Creates an API key. The raw key is returned ONCE and cannot be retrieved later.")
    public ResponseEntity<AppResponse<IssuedApiKeyResponse>> issue(@Valid @RequestBody CreateApiKeyRequest req) {
        IssuedApiKey issued = managementService.issue(toCommand(req));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AppResponse.ok(IssuedApiKeyResponse.from(issued)));
    }

    @GetMapping
    @Operation(summary = "List API keys", description = "Lists API keys for a subject without exposing the raw key.")
    public AppResponse<List<ApiKeyResponse>> list(@RequestParam("subject") String subject) {
        List<ApiKeyResponse> keys = managementService.listBySubject(subject).stream()
                .map(ApiKeyResponse::from)
                .toList();
        return AppResponse.ok(keys);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Revoke API key", description = "Revokes an API key. Takes effect on the next request.")
    public AppResponse<Void> revoke(@PathVariable("id") Long id) {
        managementService.revoke(id);
        return AppResponse.<Void>ok(null);
    }

    private CreateApiKeyCommand toCommand(CreateApiKeyRequest req) {
        return new CreateApiKeyCommand(req.subject(), req.label(), req.allowedIps(), req.expiresAt());
    }
}
