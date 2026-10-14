package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.model.TipoParticipacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TipoTicketRequest(
        @NotNull TipoParticipacion tipo,
        @Positive int stockMaximo,
        @Positive long precio,
        @Size(max = 255) String descripcion,
        @Size(max = 255) String imagenUrl
) {
}
