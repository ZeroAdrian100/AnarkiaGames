package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.model.TipoParticipacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InscripcionRequest(
        @NotNull TipoParticipacion tipo,
        @Positive Integer cantidad,
        String tokenId
) {
    public int cantidadSolicitada() {
        return cantidad == null ? 1 : cantidad;
    }
}
