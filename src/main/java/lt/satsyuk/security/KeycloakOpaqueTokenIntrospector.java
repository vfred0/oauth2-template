package lt.satsyuk.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;

import java.util.Collection;

public class KeycloakOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private final OpaqueTokenIntrospector delegate;
    private final KeycloakOpaqueRoleConverter roleConverter;

    public KeycloakOpaqueTokenIntrospector(String introspectionUrl,
                                           String clientId,
                                           String clientSecret,
                                           KeycloakOpaqueRoleConverter roleConverter) {
        this.delegate = SpringOpaqueTokenIntrospector.withIntrospectionUri(introspectionUrl)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
        this.roleConverter = roleConverter;
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        OAuth2AuthenticatedPrincipal principal = delegate.introspect(token);
        Collection<GrantedAuthority> authorities = roleConverter.convert(principal.getAttributes());
        return new DefaultOAuth2AuthenticatedPrincipal(
                principal.getName(),
                principal.getAttributes(),
                authorities
        );
    }
}
