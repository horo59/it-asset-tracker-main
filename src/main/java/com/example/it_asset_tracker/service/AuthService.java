
package com.example.it_asset_tracker.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.it_asset_tracker.dto.auth.AuthResponse;
import com.example.it_asset_tracker.dto.auth.LoginRequest;
import com.example.it_asset_tracker.dto.auth.RegisterRequest;
import com.example.it_asset_tracker.dto.auth.RegisterResponse;
import com.example.it_asset_tracker.entity.AppUser;
import com.example.it_asset_tracker.repository.AppUserRepository;
import com.example.it_asset_tracker.enums.UserRole;

@Service
public class AuthService {

    private static final long TOKEN_LIFETIME_SECONDS = 3600;

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    public AuthService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String username = request.username().trim();

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username is already taken");
        }

        AppUser user = new AppUser(
                username,
                passwordEncoder.encode(request.password()),
                UserRole.STAFF);

        AppUser savedUser = userRepository.save(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole().name());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByUsername(
                request.username().trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid username or password"));

        if (!passwordEncoder.matches(
                request.password(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username or password");
        }

        Instant now = Instant.now();

        List<String> authorities = List.of(
                "ROLE_" + user.getRole().name());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("it-asset-tracker")
                .issuedAt(now)
                .expiresAt(
                        now.plus(
                                TOKEN_LIFETIME_SECONDS,
                                ChronoUnit.SECONDS))
                .subject(user.getUsername())
                .claim("authorities", authorities)
                .build();

        JwsHeader header =
                JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims))
                .getTokenValue();

        return new AuthResponse(
                token,
                "Bearer",
                TOKEN_LIFETIME_SECONDS,
                user.getUsername(),
                user.getRole().name());
    }
}
