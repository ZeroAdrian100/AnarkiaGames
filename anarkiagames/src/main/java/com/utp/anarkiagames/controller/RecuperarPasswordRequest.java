package com.utp.anarkiagames.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecuperarPasswordRequest(
        @NotBlank @Email String email
) {
}
