package lt.satsyuk.service;

import lombok.RequiredArgsConstructor;
import lt.satsyuk.dto.UserProfileResponse;
import lt.satsyuk.model.UserRole;
import lt.satsyuk.repository.UserRoleRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPermissionService {

    private final UserRoleRepository userRoleRepository;

    @Transactional(readOnly = true)
    public List<GrantedAuthority> loadAuthorities(String keycloakSub) {
        return userRoleRepository.findByKeycloakSub(keycloakSub)
                .map(userRole -> userRole.getRole().getPermissions().stream()
                        .map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p.getResource() + ":" + p.getAction()))
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public UserProfileResponse buildProfile(String keycloakSub) {
        return userRoleRepository.findByKeycloakSub(keycloakSub)
                .map(this::toProfile)
                .orElse(new UserProfileResponse(keycloakSub, null, List.of()));
    }

    private UserProfileResponse toProfile(UserRole userRole) {
        List<String> permissions = userRole.getRole().getPermissions().stream()
                .map(p -> p.getResource() + ":" + p.getAction())
                .sorted()
                .collect(Collectors.toList());
        return new UserProfileResponse(userRole.getKeycloakSub(), userRole.getRole().getName(), permissions);
    }
}
