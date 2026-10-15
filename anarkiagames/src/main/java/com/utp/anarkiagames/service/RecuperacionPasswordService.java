package com.utp.anarkiagames.service;

import com.utp.anarkiagames.exception.SolicitudInvalidaException;
import com.utp.anarkiagames.model.TokenRecuperacion;
import com.utp.anarkiagames.model.User;
import com.utp.anarkiagames.repository.TokenRecuperacionRepository;
import com.utp.anarkiagames.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RecuperacionPasswordService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ENLACE_INVALIDO = "El enlace es inválido o expiró; solicita uno nuevo";

    private final UserRepository userRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final CorreoService correoService;

    @Value("${jwt.reset.expiration}")
    private long expiracionMs;
    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Envía un enlace de un solo uso al email, si está registrado. La respuesta al cliente es la misma
     * en ambos casos, para no revelar qué emails tienen cuenta. Un pedido nuevo invalida los anteriores.
     */
    @Transactional
    public void solicitar(final String email) {
        userRepository.findByEmail(email).ifPresent(usuario -> {
            tokenRepository.eliminarPendientes(usuario);
            final String token = nuevoToken();
            tokenRepository.save(TokenRecuperacion.builder()
                    .usuario(usuario)
                    .tokenHash(hash(token))
                    .expiraEn(LocalDateTime.now().plusNanos(expiracionMs * 1_000_000))
                    .build());
            correoService.enviarRecuperacion(usuario.getEmail(), usuario.getNombre(),
                    frontendUrl + "/restablecer-password?token=" + token, expiracionMs / 60_000);
        });
    }

    @Transactional
    public void restablecer(final String token, final String password) {
        final LocalDateTime ahora = LocalDateTime.now();
        final TokenRecuperacion recuperacion = tokenRepository.findByTokenHash(hash(token))
                .filter(t -> t.getUsadoEn() == null && t.getExpiraEn().isAfter(ahora))
                .orElseThrow(() -> new SolicitudInvalidaException(ENLACE_INVALIDO));
        if (tokenRepository.marcarUsado(recuperacion.getId(), ahora) == 0) {
            throw new SolicitudInvalidaException(ENLACE_INVALIDO);
        }
        final User usuario = userRepository.findById(recuperacion.getUsuario().getId()).orElseThrow();
        usuario.setPasswordHash(passwordEncoder.encode(password));
    }

    private static String nuevoToken() {
        final byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(final String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
