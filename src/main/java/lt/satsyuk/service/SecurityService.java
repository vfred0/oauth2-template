package lt.satsyuk.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    public String clientId() {
        var auth = (JwtAuthenticationToken)
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) return "anonymous";

        return auth.getToken().getClaimAsString("azp");
    }

    public String username() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
