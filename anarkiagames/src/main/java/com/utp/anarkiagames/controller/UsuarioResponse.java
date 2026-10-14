package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.model.Role;
import com.utp.anarkiagames.model.User;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        Role role,
        LocalDateTime createdAt
) {
    public static UsuarioResponse of(final User user) {
        return new UsuarioResponse(user.getId(), user.getNombre(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
