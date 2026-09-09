package com.utp.anarkiagames.controller;

import java.time.LocalDateTime;
import java.util.List;

public record TorneoDetalleResponse(
        Long id,
        String nombre,
        String juego,
        LocalDateTime fechaInicio,
        List<TipoTicketResponse> tiposTicket
) {
}