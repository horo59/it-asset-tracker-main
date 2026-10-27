
package com.example.it_asset_tracker.config;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${JWT_SECRET}")
    private String jwtSecret;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey jwtSecretKey() {
        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(jwtSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "JWT_SECRET must be a valid Base64-encoded secret.",
                    exception
            );
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must contain at least 32 bytes."
            );
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(
                new ImmutableSecret<>(jwtSecretKey)
        );
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        // Read authorities such as ROLE_ADMIN from the JWT.
        authoritiesConverter.setAuthoritiesClaimName("authorities");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow the React development server.
        configuration.setAllowedOrigins(
                List.of("http://localhost:5173")
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type"
                )
        );

        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            ObjectMapper objectMapper
    ) throws Exception {

        http
            // Process CORS before enforcing authentication rules.
            .cors(cors -> cors.configurationSource(
                    corsConfigurationSource()
            ))

            .csrf(csrf -> csrf.disable())

            .sessionManagement(session -> session
                    .sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS
                    )
            )

            .httpBasic(basic -> basic.disable())

            .formLogin(form -> form.disable())

            .exceptionHandling(errors -> errors
                    .authenticationEntryPoint(
                            (request, response, exception) ->
                                    writeSecurityError(
                                            objectMapper,
                                            request,
                                            response,
                                            HttpServletResponse.SC_UNAUTHORIZED,
                                            "Unauthorized",
                                            "Authentication is required."
                                    )
                    )
                    .accessDeniedHandler(
                            (request, response, exception) ->
                                    writeSecurityError(
                                            objectMapper,
                                            request,
                                            response,
                                            HttpServletResponse.SC_FORBIDDEN,
                                            "Forbidden",
                                            "You do not have permission to access this resource."
                                    )
                    )
            )

            .authorizeHttpRequests(auth -> auth

                    // Public Swagger and OpenAPI documentation.
                    .requestMatchers(
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**"
                    ).permitAll()

                    // Public registration and login.
                    .requestMatchers(
                            HttpMethod.POST,
                            "/api/auth/register"
                    ).permitAll()

                    .requestMatchers(
                            HttpMethod.POST,
                            "/api/auth/login"
                    ).permitAll()

                    // ADMIN and STAFF can view categories.
                    .requestMatchers(
                            HttpMethod.GET,
                            "/api/categories"
                    ).hasAnyRole("ADMIN", "STAFF")

                    // Only ADMIN can list staff for asset assignment.
                    .requestMatchers(
                            HttpMethod.GET,
                            "/api/users"
                    ).hasRole("ADMIN")

                    // Only ADMIN can create categories.
                    .requestMatchers(
                            HttpMethod.POST,
                            "/api/categories"
                    ).hasRole("ADMIN")

                    // ADMIN and STAFF can list assets.
                    .requestMatchers(
                            HttpMethod.GET,
                            "/api/assets",
                            "/api/assets/my-assets"
                    ).hasAnyRole("ADMIN", "STAFF")

                    // Only ADMIN can create assets.
                    .requestMatchers(
                            HttpMethod.POST,
                            "/api/assets"
                    ).hasRole("ADMIN")

                    // Only ADMIN can assign or return assets.
                    .requestMatchers(
                            HttpMethod.PATCH,
                            "/api/assets/*/assign/*",
                            "/api/assets/*/return"
                    ).hasRole("ADMIN")

                    // Only ADMIN can delete assets.
                    .requestMatchers(
                            HttpMethod.DELETE,
                            "/api/assets/*"
                    ).hasRole("ADMIN")

                    // All remaining endpoints require authentication.
                    .anyRequest().authenticated()
            )

            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt
                            .jwtAuthenticationConverter(
                                    jwtAuthenticationConverter
                            )
                    )
            );

        return http.build();
    }

    private void writeSecurityError(
            ObjectMapper objectMapper,
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String error,
            String message
    ) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");

        objectMapper.writeValue(
                response.getOutputStream(),
                Map.of(
                        "timestamp", OffsetDateTime.now(),
                        "status", status,
                        "error", error,
                        "message", message,
                        "path", request.getRequestURI()
                )
        );
    }
}
