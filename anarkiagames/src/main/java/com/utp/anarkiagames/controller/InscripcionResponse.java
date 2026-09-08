package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.model.TipoParticipacion;

import java.time.LocalDateTime;

public record InscripcionResponse(
        Long id,
        Long torneoId,
        TipoParticipacion tipo,
        LocalDateTime fechaCompra
) {
}
