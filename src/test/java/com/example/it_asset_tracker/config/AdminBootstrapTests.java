package com.example.it_asset_tracker.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.it_asset_tracker.entity.AppUser;
import com.example.it_asset_tracker.repository.AppUserRepository;
import com.example.it_asset_tracker.enums.UserRole;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTests {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createsAdminWithEncodedPasswordWhenUserDoesNotExist() throws Exception {
        AdminBootstrap bootstrap = new AdminBootstrap(
                userRepository, passwordEncoder, " admin ", "long-password-123");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("long-password-123"))
                .thenReturn("encoded-password");

        bootstrap.run(null);

        ArgumentCaptor<AppUser> userCaptor =
                ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("admin", userCaptor.getValue().getUsername());
        assertEquals("encoded-password", userCaptor.getValue().getPassword());
        assertEquals(UserRole.ADMIN, userCaptor.getValue().getRole());
    }

    @Test
    void doesNotChangeExistingAdmin() throws Exception {
        AdminBootstrap bootstrap = new AdminBootstrap(
                userRepository, passwordEncoder, "admin", "long-password-123");
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(new AppUser(
                        "admin", "existing-hash", UserRole.ADMIN)));

        bootstrap.run(null);

        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void refusesToPromoteExistingStaffUser() {
        AdminBootstrap bootstrap = new AdminBootstrap(
                userRepository, passwordEncoder, "staff", "long-password-123");
        when(userRepository.findByUsername("staff"))
                .thenReturn(Optional.of(new AppUser(
                        "staff", "existing-hash", UserRole.STAFF)));

        assertThrows(IllegalStateException.class, () -> bootstrap.run(null));
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void rejectsPartiallyConfiguredCredentials() {
        AdminBootstrap bootstrap = new AdminBootstrap(
                userRepository, passwordEncoder, "admin", "");

        assertThrows(IllegalStateException.class, () -> bootstrap.run(null));
        verify(userRepository, never()).findByUsername("admin");
    }
}
