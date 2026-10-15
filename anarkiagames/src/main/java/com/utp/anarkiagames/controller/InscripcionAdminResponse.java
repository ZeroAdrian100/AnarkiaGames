package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.model.EstadoInscripcion;
import com.utp.anarkiagames.model.Inscripcion;
import com.utp.anarkiagames.model.TipoParticipacion;

import java.time.LocalDateTime;

public record InscripcionAdminResponse(
        Long id,
        Long usuarioId,
        String usuarioNombre,
        String usuarioEmail,
        TipoParticipacion tipo,
        String gamerTag,
        String equipo,
        int cantidad,
        long monto,
        EstadoInscripcion estado,
        LocalDateTime fechaCompra,
        String referenciaPago,
        String referenciaReembolso
) {
    public static InscripcionAdminResponse of(final Inscripcion inscripcion) {
        return new InscripcionAdminResponse(
                inscripcion.getId(),
                inscripcion.getUsuario().getId(),
                inscripcion.getUsuario().getNombre(),
                inscripcion.getUsuario().getEmail(),
                inscripcion.getTipoTicket().getTipo(),
                inscripcion.getGamerTag(),
                inscripcion.getEquipo(),
                inscripcion.getCantidad(),
                inscripcion.getMonto(),
                inscripcion.getEstado(),
                inscripcion.getFechaCompra(),
                inscripcion.getReferenciaPago(),
                inscripcion.getReferenciaReembolso()
        );
    }
}
