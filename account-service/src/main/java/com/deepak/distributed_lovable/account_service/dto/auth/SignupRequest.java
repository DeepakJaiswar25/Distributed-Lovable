package com.deepak.distributed_lovable.account_service.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @NotBlank String name, @Email @NotBlank String username, @NotBlank String password
) {
}
