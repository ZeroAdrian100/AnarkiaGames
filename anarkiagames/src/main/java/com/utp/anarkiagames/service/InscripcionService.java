package com.utp.anarkiagames.service;

import com.utp.anarkiagames.controller.InscripcionRequest;
import com.utp.anarkiagames.controller.InscripcionResponse;
import com.utp.anarkiagames.controller.MiInscripcionResponse;
import com.utp.anarkiagames.exception.*;
import com.utp.anarkiagames.model.Inscripcion;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InscripcionService {
    private final InscripcionRepository inscripcionRepository;
    private final TipoTicketRepository tipoTicketRepository;
    private final TorneoRepository torneoRepository;
    private final PaymentGateway paymentGateway;

    /**
     * Compra un ticket. El torneo queda bloqueado durante toda la compra, así dos compras
     * simultáneas no pueden superar el stock. Si el cobro pasa pero la inscripción no se
     * puede guardar, el cargo se reembolsa.
     */
    @Transactional
    public InscripcionResponse comprar(final Long torneoId, final InscripcionRequest request, final User usuario) {
        final Torneo torneo = torneoRepository.findByIdForUpdate(torneoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Torneo no encontrado"));

        final TipoTicket tipoTicket = tipoTicketRepository.findByTorneoAndTipo(torneo, request.tipo())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay tickets de tipo " + request.tipo() + " configurados para este torneo"));

        inscripcionRepository.findByUsuarioAndTipoTicket(usuario, tipoTicket)
                .ifPresent(i -> { throw new ConflictoException("Ya tienes un ticket de este tipo para este torneo"); });

        final long vendidos = inscripcionRepository.countByTipoTicket(tipoTicket);
        if (vendidos >= tipoTicket.getStockMaximo()) {
            throw new ConflictoException("No hay más cupos disponibles para " + request.tipo());
        }

        final long monto = tipoTicket.getPrecio();
        final PaymentResult resultado = paymentGateway.cobrar(
                request.tokenId(),
                monto,
                usuario.getEmail(),
                "Ticket " + request.tipo() + " - " + torneo.getNombre()
        );

        if (!resultado.exitoso()) {
            throw new PagoRechazadoException("Pago rechazado: " + resultado.mensajeError());
        }

        final Inscripcion inscripcion = Inscripcion.builder()
                .usuario(usuario)
                .tipoTicket(tipoTicket)
                .fechaCompra(LocalDateTime.now())
                .culqiChargeId(resultado.referenciaId())
                .build();

        final Inscripcion guardada;
        try {
            guardada = inscripcionRepository.saveAndFlush(inscripcion);
        } catch (RuntimeException e) {
            reembolsar(resultado.referenciaId(), monto);
            throw e;
        }

        return new InscripcionResponse(
                guardada.getId(),
                torneo.getId(),
                tipoTicket.getTipo(),
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
                        i.getFechaCompra()
                ))
                .toList();
    }

    private void reembolsar(final String referenciaId, final long monto) {
        final PaymentResult reembolso = paymentGateway.reembolsar(referenciaId, monto);
        if (!reembolso.exitoso()) {
            // Queda un cobro sin ticket: hay que reembolsarlo a mano desde el panel de la pasarela.
            log.error("No se pudo reembolsar el cargo {}: {}", referenciaId, reembolso.mensajeError());
        }
    }
}
