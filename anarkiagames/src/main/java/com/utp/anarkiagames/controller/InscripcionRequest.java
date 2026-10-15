package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.model.TipoParticipacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record InscripcionRequest(
        @NotNull TipoParticipacion tipo,
        @Positive Integer cantidad,
        String tokenId,
        /* Obligatorio para COMPETIDOR; se ignora para ESPECTADOR. */
        @Size(max = 50) String gamerTag,
        /* Opcional para COMPETIDOR; se ignora para ESPECTADOR. */
        @Size(max = 100) String equipo
) {
    public int cantidadSolicitada() {
        return cantidad == null ? 1 : cantidad;
    }
}
