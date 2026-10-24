
package com.example.it_asset_tracker.config;

import com.example.it_asset_tracker.entity.AppUser;
import com.example.it_asset_tracker.enums.UserRole;
import com.example.it_asset_tracker.repository.AppUserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminBootstrap(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.username:}") String username,
            @Value("${app.bootstrap-admin.password:}") String password) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username.trim();
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {

        if (username.isEmpty() && password.isEmpty()) {
            return;
        }

        if (username.isEmpty() || password.isBlank()) {
            throw new IllegalStateException(
                    "Both APP_BOOTSTRAP_ADMIN_USERNAME and "
                            + "APP_BOOTSTRAP_ADMIN_PASSWORD must be set");
        }

        if (username.length() < 3 || username.length() > 50
                || password.length() < 12 || password.length() > 72) {
            throw new IllegalStateException(
                    "Bootstrap admin credentials must meet the normal "
                            + "username and password length requirements");
        }

        userRepository.findByUsername(username).ifPresentOrElse(
                user -> {
                    if (user.getRole() != UserRole.ADMIN) {
                        throw new IllegalStateException(
                                "Bootstrap admin username already belongs "
                                        + "to a non-admin user");
                    }

                    // Existing ADMIN accounts are left unchanged.
                },
                () -> userRepository.save(
                        new AppUser(
                                username,
                                passwordEncoder.encode(password),
                                UserRole.ADMIN))
        );
    }
}
