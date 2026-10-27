
package com.example.it_asset_tracker.dto.user;

import com.example.it_asset_tracker.entity.AppUser;

public record StaffResponse(
        Long id,
        String username
) {
    public static StaffResponse from(AppUser user) {
        return new StaffResponse(
                user.getId(),
                user.getUsername()
        );
    }
}
