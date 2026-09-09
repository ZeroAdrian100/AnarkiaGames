package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.model.TipoParticipacion;

public record TipoTicketRequest(
        TipoParticipacion tipo,
        int stockMaximo,
        long precio,
        String descripcion,
        String imagenUrl
) {
}