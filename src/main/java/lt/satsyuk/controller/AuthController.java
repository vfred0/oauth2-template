package lt.satsyuk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lt.satsyuk.dto.AppResponse;
import lt.satsyuk.dto.KeycloakTokenResponse;
import lt.satsyuk.dto.LoginRequest;
import lt.satsyuk.dto.LogoutRequest;
import lt.satsyuk.dto.RefreshRequest;
import lt.satsyuk.dto.TokenResponse;
import lt.satsyuk.exception.KeycloakAuthException;
import lt.satsyuk.security.RefreshTokenCookieWriter;
import lt.satsyuk.service.KeycloakAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Keycloak authentication endpoints")
public class AuthController {

    private final KeycloakAuthService authService;
    private final RefreshTokenCookieWriter cookieWriter;

    @PostMapping("/login")
    @Operation(summary = "Login and obtain tokens",
            description = "Authenticates user. Returns access token in body and sets refresh token as HttpOnly cookie.")
    @ApiResponse(responseCode = "200", description = "Tokens issued",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/json"))
    public ResponseEntity<AppResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest req,
            @RequestHeader(value = "DPoP", required = false) String dpopProof,
            HttpServletResponse response) {
        try {
            KeycloakTokenResponse tokens = authService.login(req, dpopProof);
            cookieWriter.write(response, tokens.getRefreshToken(), (int) tokens.getRefreshExpiresIn());
            return ResponseEntity.ok(AppResponse.ok(TokenResponse.from(tokens)));
        } catch (KeycloakAuthException ex) {
            return ResponseEntity.status(ex.getStatus())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(AppResponse.error(AppResponse.ErrorCode.UNAUTHORIZED.getCode(), ex.getKeycloakMessage()));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh tokens",
            description = "Exchanges the refresh token cookie for a new access token. Rotates the refresh token cookie.")
    @ApiResponse(responseCode = "200", description = "Tokens refreshed",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppResponse.class)))
    @ApiResponse(responseCode = "401", description = "Refresh token missing or expired",
            content = @Content(mediaType = "application/json"))
    public ResponseEntity<AppResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshRequest req,
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            @RequestHeader(value = "DPoP", required = false) String dpopProof,
            HttpServletResponse response) {
        if (!StringUtils.hasText(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(AppResponse.error(AppResponse.ErrorCode.INVALID_GRANT.getCode(), "Refresh token is missing"));
        }
        try {
            KeycloakTokenResponse tokens = authService.refresh(req, refreshToken, dpopProof);
            cookieWriter.write(response, tokens.getRefreshToken(), (int) tokens.getRefreshExpiresIn());
            return ResponseEntity.ok(AppResponse.ok(TokenResponse.from(tokens)));
        } catch (KeycloakAuthException ex) {
            return ResponseEntity.status(ex.getStatus())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(AppResponse.error(AppResponse.ErrorCode.INVALID_GRANT.getCode(), ex.getKeycloakMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes the refresh token in Keycloak and clears the refresh token cookie.")
    @ApiResponse(responseCode = "200", description = "Logged out",
            content = @Content(mediaType = "application/json"))
    public ResponseEntity<AppResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest req,
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            @RequestHeader(value = "DPoP", required = false) String dpopProof,
            HttpServletResponse response) {
        try {
            if (StringUtils.hasText(refreshToken)) {
                authService.logout(req, refreshToken, dpopProof);
            }
            cookieWriter.clear(response);
            return ResponseEntity.ok(AppResponse.<Void>ok(null));
        } catch (KeycloakAuthException ex) {
            cookieWriter.clear(response);
            return ResponseEntity.status(ex.getStatus())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(AppResponse.<Void>error(AppResponse.ErrorCode.INVALID_TOKEN.getCode(), ex.getKeycloakMessage()));
        }
    }
}
