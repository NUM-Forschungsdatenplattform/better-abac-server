package care.better.abac.oauth;

import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.Collection;
import java.util.Map;

/**
 * @author Andrej Dolenc
 */
public class DelegatingJwtBearerTokenAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();

    public DelegatingJwtBearerTokenAuthenticationConverter(
            @NonNull Converter<Jwt, Collection<GrantedAuthority>> grantedAuthoritiesConverter) {
        authenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                TokenType.BEARER, jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt());
        Map<String, Object> attributes = jwt.getClaims();

        AbstractAuthenticationToken token = authenticationConverter.convert(jwt);
        Collection<GrantedAuthority> authorities = token.getAuthorities();

        OAuth2AuthenticatedPrincipal principal = new DefaultOAuth2AuthenticatedPrincipal(attributes, authorities);
        return new BearerTokenAuthentication(principal, accessToken, authorities);
    }
}
