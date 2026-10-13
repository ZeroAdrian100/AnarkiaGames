package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.model.TipoParticipacion;
import com.utp.anarkiagames.model.TipoTicket;

public record TipoTicketResponse(
        Long id,
        Long torneoId,
        TipoParticipacion tipo,
        int stockMaximo,
        long disponibles,
        long precio,
        String descripcion,
        String imagenUrl
) {
    public static TipoTicketResponse of(final TipoTicket tipoTicket, final long vendidos) {
        return new TipoTicketResponse(
                tipoTicket.getId(),
                tipoTicket.getTorneo().getId(),
                tipoTicket.getTipo(),
                tipoTicket.getStockMaximo(),
                tipoTicket.getStockMaximo() - vendidos,
                tipoTicket.getPrecio(),
                tipoTicket.getDescripcion(),
                tipoTicket.getImagenUrl()
        );
    }
}
