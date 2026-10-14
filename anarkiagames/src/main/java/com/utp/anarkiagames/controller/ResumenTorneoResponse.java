package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.model.EstadoTorneo;
import com.utp.anarkiagames.model.TipoParticipacion;

import java.util.List;

/** Stock e ingresos de un torneo. Solo cuentan las compras pagadas; montos en céntimos. */
public record ResumenTorneoResponse(
        Long torneoId,
        String nombre,
        EstadoTorneo estado,
        List<TipoTicketResumen> tiposTicket,
        long ingresosTotales
) {
    public record TipoTicketResumen(
            Long tipoTicketId,
            TipoParticipacion tipo,
            long precio,
            int stockMaximo,
            long vendidos,
            long disponibles,
            long ingresos
    ) {
    }
}
