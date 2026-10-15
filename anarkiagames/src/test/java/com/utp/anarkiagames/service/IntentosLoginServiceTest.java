package com.utp.anarkiagames.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntentosLoginServiceTest {
    private static final String IP = "10.0.0.1";

    private Instant ahora = Instant.parse("2026-10-14T10:00:00Z");
    private final IntentosLoginService service = new IntentosLoginService(new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    });

    private void fallar(final int veces, final String email) {
        for (int i = 0; i < veces; i++) {
            service.registrarFallo(email, IP);
        }
    }

    @Test
    void bloqueaAlLlegarAlMaximoDeFallos() {
        fallar(IntentosLoginService.MAX_FALLOS - 1, "ana@mail.com");
        assertEquals(0, service.minutosBloqueado("ana@mail.com", IP));

        fallar(1, "ana@mail.com");
        assertEquals(15, service.minutosBloqueado("ana@mail.com", IP));
    }

    @Test
    void elBloqueoVenceConLaVentana() {
        fallar(IntentosLoginService.MAX_FALLOS, "ana@mail.com");
        ahora = ahora.plus(Duration.ofMinutes(14));
        assertEquals(1, service.minutosBloqueado("ana@mail.com", IP));

        ahora = ahora.plus(Duration.ofMinutes(1));
        assertEquals(0, service.minutosBloqueado("ana@mail.com", IP));
    }

    @Test
    void elEmailNoDistingueMayusculasYCadaIpCuentaAparte() {
        fallar(IntentosLoginService.MAX_FALLOS, "Ana@Mail.com ");
        assertTrue(service.minutosBloqueado("ana@mail.com", IP) > 0);
        assertEquals(0, service.minutosBloqueado("ana@mail.com", "10.0.0.2"));
    }

    @Test
    void unLoginExitosoReiniciaElContador() {
        fallar(IntentosLoginService.MAX_FALLOS - 1, "ana@mail.com");
        service.registrarExito("ana@mail.com", IP);
        fallar(IntentosLoginService.MAX_FALLOS - 1, "ana@mail.com");
        assertEquals(0, service.minutosBloqueado("ana@mail.com", IP));
    }
}
