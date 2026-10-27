
package com.example.it_asset_tracker.repository;

import com.example.it_asset_tracker.entity.AppUser;
import com.example.it_asset_tracker.enums.UserRole;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    List<AppUser> findByRole(UserRole role);
}
