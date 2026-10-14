package com.utp.anarkiagames.controller;

/**
 * Resultado de cancelar un torneo. Las compras pendientes no se pudieron reembolsar
 * y siguen PAGADA: se reintentan cancelando de nuevo o reembolsándolas una por una.
 */
public record CancelacionResponse(
        Long torneoId,
        int reembolsadas,
        int pendientes
) {
}
