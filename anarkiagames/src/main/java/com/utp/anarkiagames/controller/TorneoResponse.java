package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.model.EstadoTorneo;
import com.utp.anarkiagames.model.Torneo;

import java.time.LocalDateTime;

public record TorneoResponse(
        Long id,
        String nombre,
        String juego,
        LocalDateTime fechaInicio,
        EstadoTorneo estado
) {
    public static TorneoResponse of(final Torneo torneo) {
        return new TorneoResponse(
                torneo.getId(), torneo.getNombre(), torneo.getJuego(), torneo.getFechaInicio(), torneo.getEstado());
    }
}
