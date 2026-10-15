package com.utp.anarkiagames.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestablecerPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
