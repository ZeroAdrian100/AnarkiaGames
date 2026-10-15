package com.utp.anarkiagames.service;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cuenta los logins fallidos por email e IP. Tras {@value #MAX_FALLOS} fallos dentro de la ventana,
 * bloquea hasta que la ventana vence. Vive en memoria: se reinicia con la app y no se comparte
 * entre varias instancias.
 */
@Service
public class IntentosLoginService {
    static final int MAX_FALLOS = 5;
    static final Duration VENTANA = Duration.ofMinutes(15);
    private static final int LIMPIAR_DESDE = 10_000;

    private record Fallos(int cantidad, Instant inicio) {
    }

    private final Map<String, Fallos> fallos = new ConcurrentHashMap<>();
    private final Clock clock;

    public IntentosLoginService() {
        this(Clock.systemUTC());
    }

    IntentosLoginService(final Clock clock) {
        this.clock = clock;
    }

    /** Minutos que faltan para desbloquear, o 0 si no está bloqueado. */
    public long minutosBloqueado(final String email, final String ip) {
        final Fallos actual = vigentes(clave(email, ip));
        if (actual == null || actual.cantidad() < MAX_FALLOS) {
            return 0;
        }
        final Duration restante = Duration.between(clock.instant(), actual.inicio().plus(VENTANA));
        return Math.max(1, (restante.toSeconds() + 59) / 60);
    }

    public void registrarFallo(final String email, final String ip) {
        final Instant ahora = clock.instant();
        fallos.compute(clave(email, ip), (k, actual) ->
                actual == null || vencido(actual, ahora)
                        ? new Fallos(1, ahora)
                        : new Fallos(actual.cantidad() + 1, actual.inicio()));
        if (fallos.size() > LIMPIAR_DESDE) {
            fallos.values().removeIf(f -> vencido(f, ahora));
        }
    }

    public void registrarExito(final String email, final String ip) {
        fallos.remove(clave(email, ip));
    }

    private Fallos vigentes(final String clave) {
        final Fallos actual = fallos.get(clave);
        return actual == null || vencido(actual, clock.instant()) ? null : actual;
    }

    private static boolean vencido(final Fallos f, final Instant ahora) {
        return !ahora.isBefore(f.inicio().plus(VENTANA));
    }

    private static String clave(final String email, final String ip) {
        return email.trim().toLowerCase(Locale.ROOT) + "|" + ip;
    }
}
