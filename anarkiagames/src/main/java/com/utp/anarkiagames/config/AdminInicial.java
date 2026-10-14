package com.utp.anarkiagames.config;

import com.utp.anarkiagames.model.Role;
import com.utp.anarkiagames.model.User;
import com.utp.anarkiagames.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el primer ADMIN al arrancar, porque el registro público solo crea clientes.
 * Actúa solo si app.admin.email y app.admin.password tienen valor y todavía no existe ningún ADMIN.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInicial implements ApplicationRunner {
    private static final int LONGITUD_MINIMA_PASSWORD = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String email;
    @Value("${app.admin.password}")
    private String password;
    @Value("${app.admin.nombre}")
    private String nombre;

    @Override
    public void run(final ApplicationArguments args) {
        if (email.isBlank() || password.isBlank() || userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        if (password.length() < LONGITUD_MINIMA_PASSWORD) {
            log.warn("ADMIN_PASSWORD debe tener al menos {} caracteres; no se creó el administrador",
                    LONGITUD_MINIMA_PASSWORD);
            return;
        }
        final User admin = userRepository.findByEmail(email)
                .orElseGet(() -> User.builder()
                        .nombre(nombre)
                        .email(email)
                        .passwordHash(passwordEncoder.encode(password))
                        .build());
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Administrador inicial configurado: {}", email);
    }
}
