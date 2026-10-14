package com.utp.anarkiagames.service;

import com.utp.anarkiagames.controller.InscripcionRequest;
import com.utp.anarkiagames.controller.InscripcionResponse;
import com.utp.anarkiagames.controller.MiInscripcionResponse;
import com.utp.anarkiagames.exception.*;
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

    @Transactional
    public InscripcionResponse comprar(final Long torneoId, final InscripcionRequest request, final User usuario) {
        final int cantidad = request.cantidadSolicitada();

        final Torneo torneo = torneoRepository.findByIdForUpdate(torneoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Torneo no encontrado"));

        if (!torneo.getFechaInicio().isAfter(LocalDateTime.now())) {
            throw new ConflictoException("La venta de tickets para este torneo ya cerró");
        }

        final TipoTicket tipoTicket = tipoTicketRepository.findByTorneoAndTipo(torneo, request.tipo())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay tickets de tipo " + request.tipo() + " configurados para este torneo"));

        if (inscripcionRepository.existsConOtroTipoEnTorneo(usuario, torneo, request.tipo())) {
            throw new ConflictoException("Ya estás inscrito en este torneo con otro tipo de participación");
        }

        if (request.tipo() == TipoParticipacion.COMPETIDOR) {
            if (cantidad != 1) {
                throw new SolicitudInvalidaException("Un competidor solo puede comprar 1 ticket");
            }
            if (inscripcionRepository.existsByUsuarioAndTipoTicket(usuario, tipoTicket)) {
                throw new ConflictoException("Ya estás inscrito como competidor en este torneo");
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
                .fechaCompra(LocalDateTime.now())
                .referenciaPago(resultado.referenciaId())
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
                guardada.getCantidad(),
                guardada.getMonto(),
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
                        i.getFechaCompra()
                ))
                .toList();
    }

    private void reembolsar(final String referenciaId, final long monto) {
        final PaymentResult reembolso = paymentGateway.reembolsar(referenciaId, monto);
        if (!reembolso.exitoso()) {
            log.error("No se pudo reembolsar el cargo {}: {}", referenciaId, reembolso.mensajeError());
        }
    }
}
