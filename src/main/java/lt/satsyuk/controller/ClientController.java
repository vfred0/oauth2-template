package lt.satsyuk.controller;

import jakarta.validation.Valid;
import lt.satsyuk.api.dto.ApiResponse;
import lt.satsyuk.dto.ClientResponse;
import lt.satsyuk.dto.CreateClientRequest;
import lt.satsyuk.service.ClientService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT_CREATE')")
    public ApiResponse<ClientResponse> create(@Valid @RequestBody CreateClientRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT_GET')")
    public ApiResponse<ClientResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }
}