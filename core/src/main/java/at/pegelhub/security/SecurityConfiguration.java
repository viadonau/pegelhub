package at.pegelhub.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import static at.pegelhub.security.PegelHubAuthority.MEASUREMENT_READ;
import static at.pegelhub.security.PegelHubAuthority.MEASUREMENT_WRITE;
import static at.pegelhub.security.PegelHubAuthority.METADATA_READ;
import static at.pegelhub.security.PegelHubAuthority.METADATA_WRITE;
import static at.pegelhub.security.PegelHubAuthority.SYSTEM_ADMIN;
import static at.pegelhub.security.PegelHubAuthority.TELEMETRY_READ;
import static at.pegelhub.security.PegelHubAuthority.TELEMETRY_WRITE;

@Configuration
@EnableConfigurationProperties(PegelHubSecurityProperties.class)
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthorityMapper jwtAuthorityMapper) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/measurements/system-time").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/connectors").hasAuthority(SYSTEM_ADMIN.value())
                        .requestMatchers(HttpMethod.POST, "/api/v1/measurements").hasAuthority(MEASUREMENT_WRITE.value())
                        .requestMatchers(HttpMethod.GET, "/api/v1/time-series/*/measurements").hasAnyAuthority(MEASUREMENT_READ.value(), SYSTEM_ADMIN.value())
                        .requestMatchers(HttpMethod.GET, "/api/v1/time-series/*/measurements/**").hasAnyAuthority(MEASUREMENT_READ.value(), SYSTEM_ADMIN.value())
                        .requestMatchers(HttpMethod.POST, "/api/v1/telemetry").hasAnyAuthority(TELEMETRY_WRITE.value(), SYSTEM_ADMIN.value())
                        .requestMatchers(HttpMethod.GET, "/api/v1/telemetry/**").hasAnyAuthority(TELEMETRY_READ.value(), SYSTEM_ADMIN.value())
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAnyAuthority(METADATA_READ.value(), METADATA_WRITE.value(), SYSTEM_ADMIN.value())
                        .requestMatchers(HttpMethod.POST, "/api/v1/**").hasAnyAuthority(METADATA_WRITE.value(), SYSTEM_ADMIN.value())
                        .requestMatchers(HttpMethod.PUT, "/api/v1/**").hasAnyAuthority(METADATA_WRITE.value(), SYSTEM_ADMIN.value())
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/**").hasAnyAuthority(METADATA_WRITE.value(), SYSTEM_ADMIN.value())
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasAnyAuthority(METADATA_WRITE.value(), SYSTEM_ADMIN.value())
                        .requestMatchers("/actuator/**").hasAuthority(SYSTEM_ADMIN.value())
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthorityMapper.authenticationConverter())))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(PegelHubSecurityProperties properties) {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(properties.issuerUri());
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(properties.issuerUri());
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(PegelHubSecurityProperties.API_AUDIENCE);
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return jwtDecoder;
    }
}
