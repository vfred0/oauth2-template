package lt.satsyuk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lt.satsyuk.dto.AppResponse;
import lt.satsyuk.dto.UserProfileResponse;
import lt.satsyuk.service.UserPermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "Me", description = "Current user profile")
public class MeController {

    private final UserPermissionService permissionService;

    @GetMapping
    @Operation(summary = "Get current user profile", description = "Returns the authenticated user's role and permissions.")
    public ResponseEntity<AppResponse<UserProfileResponse>> me(Authentication authentication) {
        UserProfileResponse profile = permissionService.buildProfile(authentication.getName());
        return ResponseEntity.ok(AppResponse.ok(profile));
    }
}
