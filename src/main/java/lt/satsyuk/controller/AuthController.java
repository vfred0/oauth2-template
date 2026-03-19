package lt.satsyuk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lt.satsyuk.dto.AppResponse;
import lt.satsyuk.service.KeycloakAuthService;
import lt.satsyuk.dto.KeycloakTokenResponse;
import lt.satsyuk.dto.LoginRequest;
import lt.satsyuk.dto.LogoutRequest;
import lt.satsyuk.dto.RefreshRequest;
import lt.satsyuk.exception.KeycloakAuthException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Keycloak authentication endpoints")
public class AuthController {

    private final KeycloakAuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login and obtain tokens", description = "Authenticates user and returns access/refresh tokens.")
    @ApiResponse(responseCode = "200", description = "Tokens issued",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AppResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/json"))
    public ResponseEntity<AppResponse<KeycloakTokenResponse>> login(@Valid @RequestBody LoginRequest req) {
        try {
            KeycloakTokenResponse tokens = authService.login(req);
            return ResponseEntity.ok(AppResponse.ok(tokens));
        } catch (KeycloakAuthException ex) {
            return ResponseEntity
                    .status(ex.getStatus())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(AppResponse.error(AppResponse.ErrorCode.UNAUTHORIZED.getCode(), ex.getKeycloakMessage()));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh tokens", description = "Exchanges refresh token for a new access token.")
    @ApiResponse(responseCode = "200", description = "Tokens refreshed",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AppResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid grant",
            content = @Content(mediaType = "application/json"))
    public ResponseEntity<AppResponse<KeycloakTokenResponse>> refresh(@Valid @RequestBody RefreshRequest req) {
        try {
            KeycloakTokenResponse tokens = authService.refresh(req);
            return ResponseEntity.ok(AppResponse.ok(tokens));
        } catch (KeycloakAuthException ex) {
            return ResponseEntity
                    .status(ex.getStatus())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(AppResponse.error(AppResponse.ErrorCode.INVALID_GRANT.getCode(), ex.getKeycloakMessage()));
        }
    }

    // Keycloak 26 revoke: 200 OK даже при ошибке
    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes the refresh token in Keycloak.")
    @ApiResponse(responseCode = "200", description = "Logged out",
            content = @Content(mediaType = "application/json"))
    @ApiResponse(responseCode = "400", description = "Invalid token",
            content = @Content(mediaType = "application/json"))
    public ResponseEntity<AppResponse<Void>> logout(@Valid @RequestBody LogoutRequest req) {
        try {
            authService.logout(req);
            return ResponseEntity.ok(AppResponse.<Void>ok(null));
        } catch (KeycloakAuthException ex) {
            return ResponseEntity
                    .status(ex.getStatus())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(AppResponse.<Void>error(AppResponse.ErrorCode.INVALID_TOKEN.getCode(), ex.getKeycloakMessage()));
        }
    }
}