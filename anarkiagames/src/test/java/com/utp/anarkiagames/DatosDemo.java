package com.utp.anarkiagames;

import com.utp.anarkiagames.model.TipoParticipacion;
import com.utp.anarkiagames.model.TipoTicket;
import com.utp.anarkiagames.model.Torneo;
import com.utp.anarkiagames.repository.TipoTicketRepository;
import com.utp.anarkiagames.repository.TorneoRepository;
import com.utp.anarkiagames.service.TorneoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

/** Carga los torneos del diseño de Figma en el perfil demo. Solo actúa sobre H2. */
@Slf4j
@Component
@Profile("demo")
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
class DatosDemo implements ApplicationRunner {
    private record Ejemplo(String nombre, String juego, String imagen, String descripcion) {
    }

    private static final Ejemplo[] TORNEOS = {
            new Ejemplo("Counter Strike 2", "Counter-Strike 2", "counter-strike-2",
                    "Accede al torneo competitivo de Counter-Strike y demuestra tus habilidades en el campo de batalla."),
            new Ejemplo("Super Smash Bros Ultimate", "Super Smash Bros. Ultimate", "super-smash-bros-ultimate",
                    "Asegura tu lugar en el torneo de Super Smash Bros. Ultimate y demuestra quién es el verdadero campeón."),
            new Ejemplo("FIFA25", "EA Sports FC 25", "fifa-25",
                    "Participa en el torneo de FIFA y demuestra tu talento en la cancha virtual."),
            new Ejemplo("Overwatch 2", "Overwatch 2", "overwatch-2",
                    "Únete al torneo de Overwatch y demuestra tu habilidad en el campo de batalla."),
            new Ejemplo("Fortnite", "Fortnite", "fortnite",
                    "Participa en el torneo de Fortnite y demuestra que eres el último en pie."),
            new Ejemplo("League of Legends", "League of Legends", "league-of-legends",
                    "Participa en el torneo de League of Legends y lleva a tu equipo a la victoria."),
            new Ejemplo("Rocket League", "Rocket League", "rocket-league",
                    "Participa en el torneo de Rocket League y demuestra tu habilidad al volante."),
    };

    private final DataSource dataSource;
    private final TorneoRepository torneoRepository;
    private final TipoTicketRepository tipoTicketRepository;
    private final TorneoService torneoService;

    @Override
    public void run(final ApplicationArguments args) throws SQLException {
        try (Connection conexion = dataSource.getConnection()) {
            final String url = conexion.getMetaData().getURL();
            if (!url.startsWith("jdbc:h2:mem:")) {
                throw new IllegalStateException("El perfil demo solo carga datos en H2 en memoria, no en " + url);
            }
        }
        if (torneoRepository.count() > 0) {
            return;
        }

        Torneo ultimo = null;
        for (int i = 0; i < TORNEOS.length; i++) {
            final Ejemplo ejemplo = TORNEOS[i];
            ultimo = torneoRepository.save(Torneo.builder()
                    .nombre(ejemplo.nombre())
                    .juego(ejemplo.juego())
                    .fechaInicio(LocalDate.now().plusDays(20L + i).atTime(LocalTime.of(18, 0)))
                    .descripcion(ejemplo.descripcion())
                    .imagenUrl("/torneos/" + ejemplo.imagen() + ".webp")
                    .lugar("Jockey Plaza del Perú")
                    .duracionHoras(8)
                    .build());
            tipoTicketRepository.save(TipoTicket.builder().torneo(ultimo).tipo(TipoParticipacion.COMPETIDOR)
                    .stockMaximo(32).precio(5000).build());
            if (!ejemplo.imagen().equals("fifa-25")) {
                tipoTicketRepository.save(TipoTicket.builder().torneo(ultimo).tipo(TipoParticipacion.ESPECTADOR)
                        .stockMaximo(200).precio(2500).build());
            }
        }
        torneoService.cancelar(ultimo.getId());
        log.info("Datos de demo cargados: {} torneos (el último, cancelado). Admin: admin@demo.com / demo12345",
                TORNEOS.length);
    }
}
