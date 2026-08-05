package com.utp.anarkiagames.controller;

public record LoginRequest(
        String email,
        String password
) {
}
