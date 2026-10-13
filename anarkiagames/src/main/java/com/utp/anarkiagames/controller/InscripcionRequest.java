package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.model.TipoParticipacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InscripcionRequest(
        @NotNull TipoParticipacion tipo,
        /* Opcional: si no se envía se compra 1 ticket. */
        @Positive Integer cantidad,
        String tokenId
) {
    public int cantidadSolicitada() {
        return cantidad == null ? 1 : cantidad;
    }
}
