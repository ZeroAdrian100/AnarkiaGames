package com.utp.anarkiagames.controller;

import org.springframework.data.domain.Page;

import java.util.List;

/** Página de resultados con un formato estable para el frontend. */
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {
    public static <T> PaginaResponse<T> of(final Page<T> page) {
        return new PaginaResponse<>(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
