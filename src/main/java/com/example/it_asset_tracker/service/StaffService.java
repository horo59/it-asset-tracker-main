
package com.example.it_asset_tracker.service;

import com.example.it_asset_tracker.dto.user.StaffResponse;
import com.example.it_asset_tracker.enums.UserRole;
import com.example.it_asset_tracker.repository.AppUserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StaffService {

    private final AppUserRepository userRepository;

    public StaffService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> getAllStaff() {
        return userRepository.findByRole(UserRole.STAFF)
                .stream()
                .map(StaffResponse::from)
                .toList();
    }
}
