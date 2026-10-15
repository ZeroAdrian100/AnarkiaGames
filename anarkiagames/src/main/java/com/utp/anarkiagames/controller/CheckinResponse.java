package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.model.TipoParticipacion;

import java.time.LocalDateTime;

public record CheckinResponse(
        String codigo,
        Long torneoId,
        String torneoNombre,
        TipoParticipacion tipo,
        String usuarioNombre,
        LocalDateTime fechaUso
) {
}
