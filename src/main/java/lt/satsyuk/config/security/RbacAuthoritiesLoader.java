package lt.satsyuk.config.security;

import lombok.RequiredArgsConstructor;
import lt.satsyuk.service.core.rbac.UserPermissionService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class RbacAuthoritiesLoader {

    private final UserPermissionService userPermissionService;

    public Collection<GrantedAuthority> loadFor(String subject) {
        return userPermissionService.loadAuthorities(subject);
    }
}
