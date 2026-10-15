package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.model.Ticket;

import java.time.LocalDateTime;

/** El frontend dibuja el QR a partir de {@code codigo}. */
public record TicketResponse(
        String codigo,
        boolean valido,
        LocalDateTime fechaUso
) {
    public static TicketResponse of(final Ticket ticket) {
        return new TicketResponse(ticket.getCodigo(), ticket.esValido(), ticket.getFechaUso());
    }
}
