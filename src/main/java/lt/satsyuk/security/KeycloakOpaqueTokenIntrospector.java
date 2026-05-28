package lt.satsyuk.security;

import lt.satsyuk.service.UserPermissionService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;

import java.util.ArrayList;
import java.util.Collection;

public class KeycloakOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private final OpaqueTokenIntrospector delegate;
    private final KeycloakOpaqueRoleConverter roleConverter;
    private final UserPermissionService userPermissionService;

    public KeycloakOpaqueTokenIntrospector(String introspectionUrl,
                                           String clientId,
                                           String clientSecret,
                                           KeycloakOpaqueRoleConverter roleConverter,
                                           UserPermissionService userPermissionService) {
        this.delegate = SpringOpaqueTokenIntrospector.withIntrospectionUri(introspectionUrl)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
        this.roleConverter = roleConverter;
        this.userPermissionService = userPermissionService;
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        OAuth2AuthenticatedPrincipal principal = delegate.introspect(token);

        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(roleConverter.convert(principal.getAttributes()));
        authorities.addAll(userPermissionService.loadAuthorities(principal.getName()));

        return new DefaultOAuth2AuthenticatedPrincipal(
                principal.getName(),
                principal.getAttributes(),
                authorities
        );
    }
}
