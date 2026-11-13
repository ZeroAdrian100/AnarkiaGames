package com.utp.anarkiagames.dto.request;

import com.utp.anarkiagames.model.Role;
import jakarta.validation.constraints.NotNull;

public record CambiarRolRequest(
        @NotNull Role role
) {
}
