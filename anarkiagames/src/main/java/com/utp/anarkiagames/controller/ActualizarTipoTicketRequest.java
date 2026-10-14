package com.utp.anarkiagames.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** El tipo de participación no se puede cambiar; para eso se elimina y se crea otro. */
public record ActualizarTipoTicketRequest(
        @Positive int stockMaximo,
        /* Precio en céntimos. Solo afecta a las compras futuras. */
        @Positive long precio,
        @Size(max = 255) String descripcion,
        @Size(max = 255) String imagenUrl
) {
}
