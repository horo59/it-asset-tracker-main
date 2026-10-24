
package com.example.it_asset_tracker.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50,
                message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 72,
                message = "Password must be between 12 and 72 characters")
        String password

) {
    public RegisterRequest {
        if (username != null) {
            username = username.trim();
        }
    }
}
