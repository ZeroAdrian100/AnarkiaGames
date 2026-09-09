package com.utp.anarkiagames.controller;

import java.time.LocalDateTime;

public record TorneoResponse(
        Long id,
        String nombre,
        String juego,
        LocalDateTime fechaInicio
) {
}
