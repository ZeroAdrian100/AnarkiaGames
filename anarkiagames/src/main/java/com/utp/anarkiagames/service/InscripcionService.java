package com.utp.anarkiagames.service;

import com.utp.anarkiagames.controller.CancelacionResponse;
import com.utp.anarkiagames.controller.InscripcionAdminResponse;
import com.utp.anarkiagames.controller.InscripcionRequest;
import com.utp.anarkiagames.controller.InscripcionResponse;
import com.utp.anarkiagames.controller.MiInscripcionResponse;
import com.utp.anarkiagames.controller.PaginaResponse;
import com.utp.anarkiagames.exception.*;
import com.utp.anarkiagames.model.EstadoInscripcion;
import com.utp.anarkiagames.model.EstadoTorneo;
import com.utp.anarkiagames.model.Inscripcion;
import com.utp.anarkiagames.model.TipoParticipacion;
import com.utp.anarkiagames.model.TipoTicket;
import com.utp.anarkiagames.model.Torneo;
import com.utp.anarkiagames.model.User;
import com.utp.anarkiagames.repository.InscripcionRepository;
import com.utp.anarkiagames.repository.TipoTicketRepository;
import com.utp.anarkiagames.repository.TorneoRepository;
import com.utp.anarkiagames.service.payment.PaymentGateway;
import com.utp.anarkiagames.service.payment.PaymentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InscripcionService {
    public static final int MAX_ESPECTADOR_POR_COMPRA = 5;

    private final InscripcionRepository inscripcionRepository;
    private final TipoTicketRepository tipoTicketRepository;
    private final TorneoRepository torneoRepository;
    private final PaymentGateway paymentGateway;
    private final TicketService ticketService;
    private final ApplicationEventPublisher eventos;

    /**
     * Compra tickets de un torneo. Reglas:
     * <ul>
     *   <li>Solo en torneos activos y antes de su fecha de inicio.</li>
     *   <li>Un usuario participa en un torneo como COMPETIDOR o como ESPECTADOR, nunca ambos.</li>
     *   <li>COMPETIDOR: un único ticket. ESPECTADOR: hasta {@value #MAX_ESPECTADOR_POR_COMPRA} por compra
     *       y puede volver a comprar.</li>
     * </ul>
     * El torneo queda bloqueado durante toda la compra, así dos compras simultáneas no pueden
     * superar el stock. Si el cobro pasa pero la inscripción no se puede guardar, el cargo se reembolsa.
     */
    @Transactional
    public InscripcionResponse comprar(final Long torneoId, final InscripcionRequest request, final User usuario) {
        final int cantidad = request.cantidadSolicitada();

        final Torneo torneo = torneoRepository.findByIdForUpdate(torneoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Torneo no encontrado"));

        if (torneo.getEstado() == EstadoTorneo.CANCELADO) {
            throw new ConflictoException("El torneo fue cancelado");
        }
        if (!torneo.getFechaInicio().isAfter(LocalDateTime.now())) {
            throw new ConflictoException("La venta de tickets para este torneo ya cerró");
        }

        final TipoTicket tipoTicket = tipoTicketRepository.findByTorneoAndTipo(torneo, request.tipo())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay tickets de tipo " + request.tipo() + " configurados para este torneo"));

        if (inscripcionRepository.existsConOtroTipoEnTorneo(usuario, torneo, request.tipo())) {
            throw new ConflictoException("Ya estás inscrito en este torneo con otro tipo de participación");
        }

        final boolean esCompetidor = request.tipo() == TipoParticipacion.COMPETIDOR;
        final String gamerTag = esCompetidor ? limpiar(request.gamerTag()) : null;
        if (esCompetidor) {
            if (cantidad != 1) {
                throw new SolicitudInvalidaException("Un competidor solo puede comprar 1 ticket");
            }
            if (gamerTag == null) {
                throw new SolicitudInvalidaException("El gamer tag es obligatorio para competir");
            }
            if (inscripcionRepository.existsByUsuarioAndTipoTicketAndEstado(usuario, tipoTicket, EstadoInscripcion.PAGADA)) {
                throw new ConflictoException("Ya estás inscrito como competidor en este torneo");
            }
            if (inscripcionRepository.existsGamerTagEnTorneo(torneo, gamerTag)) {
                throw new ConflictoException("El gamer tag " + gamerTag + " ya está inscrito en este torneo");
            }
        } else if (cantidad > MAX_ESPECTADOR_POR_COMPRA) {
            throw new SolicitudInvalidaException(
                    "Máximo " + MAX_ESPECTADOR_POR_COMPRA + " tickets de espectador por compra");
        }

        final long disponibles = tipoTicket.getStockMaximo() - inscripcionRepository.sumCantidadByTipoTicket(tipoTicket);
        if (cantidad > disponibles) {
            throw new ConflictoException(disponibles <= 0
                    ? "No hay más cupos disponibles para " + request.tipo()
                    : "Solo quedan " + disponibles + " cupos para " + request.tipo());
        }

        final long monto = Math.multiplyExact(tipoTicket.getPrecio(), cantidad);
        final PaymentResult resultado = paymentGateway.cobrar(
                request.tokenId(),
                monto,
                usuario.getEmail(),
                cantidad + " ticket(s) " + request.tipo() + " - " + torneo.getNombre()
        );

        if (!resultado.exitoso()) {
            throw new PagoRechazadoException("Pago rechazado: " + resultado.mensajeError());
        }

        final Inscripcion inscripcion = Inscripcion.builder()
                .usuario(usuario)
                .tipoTicket(tipoTicket)
                .cantidad(cantidad)
                .monto(monto)
                .gamerTag(gamerTag)
                .equipo(esCompetidor ? limpiar(request.equipo()) : null)
                .fechaCompra(LocalDateTime.now())
                .referenciaPago(resultado.referenciaId())
                .build();

        final Inscripcion guardada;
        final List<String> codigos;
        try {
            guardada = inscripcionRepository.saveAndFlush(inscripcion);
            codigos = ticketService.generar(guardada);
        } catch (RuntimeException e) {
            reembolsar(resultado.referenciaId(), monto);
            throw e;
        }

        eventos.publishEvent(new CompraConfirmadaEvent(
                usuario.getEmail(), usuario.getNombre(), torneo.getNombre(), torneo.getJuego(),
                torneo.getFechaInicio(), tipoTicket.getTipo(), cantidad, monto, codigos));

        return new InscripcionResponse(
                guardada.getId(),
                torneo.getId(),
                tipoTicket.getTipo(),
                guardada.getCantidad(),
                guardada.getMonto(),
                guardada.getGamerTag(),
                guardada.getEquipo(),
                guardada.getFechaCompra()
        );
    }

    public List<MiInscripcionResponse> listarMisInscripciones(final User usuario) {
        return inscripcionRepository.findByUsuario(usuario).stream()
                .map(i -> new MiInscripcionResponse(
                        i.getId(),
                        i.getTipoTicket().getTorneo().getId(),
                        i.getTipoTicket().getTorneo().getNombre(),
                        i.getTipoTicket().getTorneo().getJuego(),
                        i.getTipoTicket().getTorneo().getFechaInicio(),
                        i.getTipoTicket().getTipo(),
                        i.getCantidad(),
                        i.getEstado(),
                        i.getGamerTag(),
                        i.getEquipo(),
                        i.getFechaCompra()
                ))
                .toList();
    }

    public PaginaResponse<InscripcionAdminResponse> listarPorTorneo(final Long torneoId, final Pageable pageable) {
        final Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Torneo no encontrado"));
        return PaginaResponse.of(inscripcionRepository.findByTipoTicketTorneo(torneo, pageable)
                .map(InscripcionAdminResponse::of));
    }

    /** Reembolsa una compra pagada. Si la pasarela falla, la compra sigue PAGADA. */
    public InscripcionAdminResponse reembolsar(final Long inscripcionId) {
        final Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripción no encontrada"));
        if (!reservarReembolso(inscripcion)) {
            throw new ConflictoException("La compra ya fue reembolsada");
        }
        if (!ejecutarReembolso(inscripcion)) {
            throw new ErrorPasarelaException("La pasarela no pudo reembolsar la compra; intenta de nuevo");
        }
        return InscripcionAdminResponse.of(inscripcionRepository.findById(inscripcionId).orElseThrow());
    }

    /**
     * Reembolsa todas las compras pagadas de un torneo. Cada reembolso se guarda por separado,
     * así un fallo no deshace los reembolsos ya hechos en la pasarela.
     */
    public CancelacionResponse reembolsarTodas(final Torneo torneo) {
        int reembolsadas = 0;
        int pendientes = 0;
        for (final Inscripcion inscripcion
                : inscripcionRepository.findByTipoTicketTorneoAndEstado(torneo, EstadoInscripcion.PAGADA)) {
            if (!reservarReembolso(inscripcion)) {
                continue; // otro proceso la reembolsó mientras tanto
            }
            if (ejecutarReembolso(inscripcion)) {
                reembolsadas++;
            } else {
                pendientes++;
            }
        }
        return new CancelacionResponse(torneo.getId(), reembolsadas, pendientes);
    }

    // Marca la compra como REEMBOLSADA antes de llamar a la pasarela, así dos reembolsos
    // simultáneos de la misma compra no cobran dos veces: solo uno gana el cambio de estado.
    private boolean reservarReembolso(final Inscripcion inscripcion) {
        return inscripcionRepository.cambiarEstado(
                inscripcion.getId(), EstadoInscripcion.PAGADA, EstadoInscripcion.REEMBOLSADA) == 1;
    }

    private boolean ejecutarReembolso(final Inscripcion inscripcion) {
        final PaymentResult reembolso = paymentGateway.reembolsar(inscripcion.getReferenciaPago(), inscripcion.getMonto());
        if (!reembolso.exitoso()) {
            log.warn("No se pudo reembolsar la inscripción {}: {}", inscripcion.getId(), reembolso.mensajeError());
            inscripcionRepository.cambiarEstado(
                    inscripcion.getId(), EstadoInscripcion.REEMBOLSADA, EstadoInscripcion.PAGADA);
            return false;
        }
        inscripcionRepository.guardarReferenciaReembolso(inscripcion.getId(), reembolso.referenciaId());
        return true;
    }

    private static String limpiar(final String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private void reembolsar(final String referenciaId, final long monto) {
        final PaymentResult reembolso = paymentGateway.reembolsar(referenciaId, monto);
        if (!reembolso.exitoso()) {
            // Queda un cobro sin ticket: hay que reembolsarlo a mano desde el panel de la pasarela.
            log.error("No se pudo reembolsar el cargo {}: {}", referenciaId, reembolso.mensajeError());
        }
    }
}
