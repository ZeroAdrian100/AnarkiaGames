package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.model.EstadoTorneo;

import java.time.LocalDateTime;
import java.util.List;

public record TorneoDetalleResponse(
        Long id,
        String nombre,
        String juego,
        LocalDateTime fechaInicio,
        EstadoTorneo estado,
        List<TipoTicketResponse> tiposTicket
) {
}
