package com.utp.anarkiagames.service;

import com.utp.anarkiagames.controller.CheckinResponse;
import com.utp.anarkiagames.controller.TicketResponse;
import com.utp.anarkiagames.exception.ConflictoException;
import com.utp.anarkiagames.exception.RecursoNoEncontradoException;
import com.utp.anarkiagames.model.EstadoInscripcion;
import com.utp.anarkiagames.model.EstadoTorneo;
import com.utp.anarkiagames.model.Inscripcion;
import com.utp.anarkiagames.model.Ticket;
import com.utp.anarkiagames.model.Torneo;
import com.utp.anarkiagames.model.User;
import com.utp.anarkiagames.repository.InscripcionRepository;
import com.utp.anarkiagames.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class TicketService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TicketRepository ticketRepository;
    private final InscripcionRepository inscripcionRepository;

    /** Crea un ticket por cada unidad de la compra. Debe llamarse dentro de la transacción de la compra. */
    public List<String> generar(final Inscripcion inscripcion) {
        final List<Ticket> tickets = IntStream.range(0, inscripcion.getCantidad())
                .mapToObj(i -> Ticket.builder().inscripcion(inscripcion).codigo(nuevoCodigo()).build())
                .toList();
        ticketRepository.saveAllAndFlush(tickets);
        return tickets.stream().map(Ticket::getCodigo).toList();
    }

    /** Tickets de una compra del usuario. Si la compra es de otro usuario responde 404, sin revelar que existe. */
    public List<TicketResponse> listarDeInscripcion(final Long inscripcionId, final User usuario) {
        final Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
                .filter(i -> i.getUsuario().getId().equals(usuario.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripción no encontrada"));
        return ticketRepository.findByInscripcionOrderByIdAsc(inscripcion).stream()
                .map(TicketResponse::of)
                .toList();
    }

    public CheckinResponse checkin(final String codigo) {
        final Ticket ticket = ticketRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ticket no encontrado"));
        final Inscripcion inscripcion = ticket.getInscripcion();
        final Torneo torneo = inscripcion.getTipoTicket().getTorneo();

        if (inscripcion.getEstado() == EstadoInscripcion.REEMBOLSADA) {
            throw new ConflictoException("El ticket no es válido: la compra fue reembolsada");
        }
        if (torneo.getEstado() == EstadoTorneo.CANCELADO) {
            throw new ConflictoException("El ticket no es válido: el torneo fue cancelado");
        }
        final LocalDateTime ahora = LocalDateTime.now();
        if (ticket.getFechaUso() != null || ticketRepository.marcarUsado(ticket.getId(), ahora) == 0) {
            throw new ConflictoException("El ticket ya fue usado");
        }
        return new CheckinResponse(
                ticket.getCodigo(),
                torneo.getId(),
                torneo.getNombre(),
                inscripcion.getTipoTicket().getTipo(),
                inscripcion.getUsuario().getNombre(),
                ahora
        );
    }

    private static String nuevoCodigo() {
        final byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
