package com.utp.anarkiagames.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        // BCrypt solo usa los primeros 72 bytes.
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String name
) {
}
