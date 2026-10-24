
package com.example.it_asset_tracker.dto.auth;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String username,
        String role
) {
}
