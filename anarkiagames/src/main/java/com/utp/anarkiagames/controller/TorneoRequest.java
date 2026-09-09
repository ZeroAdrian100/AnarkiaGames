package com.utp.anarkiagames.controller;

import java.time.LocalDateTime;

public record TorneoRequest (
        String nombre,
        String juego,
        LocalDateTime fechaInicio
){
}
