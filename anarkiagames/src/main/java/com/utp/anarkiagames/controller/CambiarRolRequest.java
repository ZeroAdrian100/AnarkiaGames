package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.model.Role;
import jakarta.validation.constraints.NotNull;

public record CambiarRolRequest(
        @NotNull Role role
) {
}
