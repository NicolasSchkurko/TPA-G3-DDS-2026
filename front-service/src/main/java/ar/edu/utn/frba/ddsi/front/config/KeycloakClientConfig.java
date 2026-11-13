package ar.edu.utn.frba.ddsi.front.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.AuthenticationMethod;

@Configuration
public class KeycloakClientConfig {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(
            @Value("${keycloak.client-id}") String clientId,
            @Value("${keycloak.client-secret}") String clientSecret,
            @Value("${keycloak.issuer-uri}") String issuerUri,
            @Value("${keycloak.authorization-uri}") String authorizationUri,
            @Value("${keycloak.token-uri}") String tokenUri,
            @Value("${keycloak.jwk-set-uri}") String jwkSetUri,
            @Value("${keycloak.user-info-uri}") String userInfoUri
    ) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope(OidcScopes.OPENID, OidcScopes.PROFILE, OidcScopes.EMAIL)
                .authorizationUri(authorizationUri)
                .tokenUri(tokenUri)
                .userInfoUri(userInfoUri)
                .userInfoAuthenticationMethod(AuthenticationMethod.HEADER)
                .userNameAttributeName("preferred_username")
                .jwkSetUri(jwkSetUri)
                .issuerUri(issuerUri)
                .clientName("DDSI")
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

}
