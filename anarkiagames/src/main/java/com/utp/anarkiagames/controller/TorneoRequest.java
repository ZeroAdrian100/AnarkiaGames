package com.utp.anarkiagames.controller;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record TorneoRequest (
        @NotBlank @Size(max = 255) String nombre,
        @NotBlank @Size(max = 255) String juego,
        @NotNull @Future LocalDateTime fechaInicio
){
}
