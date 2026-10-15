package com.utp.anarkiagames.service;

import com.utp.anarkiagames.model.TipoParticipacion;

import java.time.LocalDateTime;
import java.util.List;

/** Se publica al confirmar una compra; el correo se envía después del commit. */
public record CompraConfirmadaEvent(
        String email,
        String nombre,
        String torneo,
        String juego,
        LocalDateTime fechaInicio,
        TipoParticipacion tipo,
        int cantidad,
        long monto,
        List<String> codigos
) {
}
