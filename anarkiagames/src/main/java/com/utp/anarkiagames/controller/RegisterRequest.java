package com.utp.anarkiagames.controller;

public record RegisterRequest(
        String email,
        String password,
        String name
) {
}
