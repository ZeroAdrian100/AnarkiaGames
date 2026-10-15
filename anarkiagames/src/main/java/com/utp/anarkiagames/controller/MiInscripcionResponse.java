package com.utp.anarkiagames.controller;


import com.utp.anarkiagames.model.EstadoInscripcion;
import com.utp.anarkiagames.model.TipoParticipacion;

import java.time.LocalDateTime;

public record MiInscripcionResponse(
        Long inscripcionId,
        Long torneoId,
        String torneoNombre,
        String juego,
        LocalDateTime fechaInicioTorneo,
        TipoParticipacion tipo,
        int cantidad,
        EstadoInscripcion estado,
        String gamerTag,
        String equipo,
        LocalDateTime fechaCompra
) {
}
