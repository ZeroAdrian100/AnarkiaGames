package com.utp.anarkiagames.service;

import com.utp.anarkiagames.controller.InscripcionRequest;
import com.utp.anarkiagames.controller.InscripcionResponse;
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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InscripcionService {
    private final InscripcionRepository inscripcionRepository;
    private final TipoTicketRepository tipoTicketRepository;
    private final TorneoRepository torneoRepository;
    private final PaymentGateway paymentGateway;

    public InscripcionResponse comprar(final Long torneoId, final InscripcionRequest request, final User usuario) throws Exception {
        final Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(()-> new IllegalArgumentException("Torneo no encontrado"));

        final TipoTicket tipoTicket = tipoTicketRepository.findByTorneoAndTipo(torneo, request.tipo())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay tickets de tipo " + request.tipo() + " configurados para este torneo"));

        inscripcionRepository.findByUsuarioAndTipoTicket(usuario, tipoTicket)
                .ifPresent(i -> { throw new IllegalStateException("Ya tienes un ticket de este tipo para este torneo"); });

        final long vendidos = inscripcionRepository.countByTipoTicket(tipoTicket);
        if(vendidos >= tipoTicket.getStockMaximo()){
            throw new IllegalStateException("No hay mas tickets disponibles para "+request.tipo());
        }

        final PaymentResult resultado = paymentGateway.cobrar(
                request.tokenId(),
                tipoTicket.getPrecio(),
                usuario.getEmail(),
                "Ticket " + request.tipo() + " - " + torneo.getNombre()
        );

        if (!resultado.exitoso()) {
            throw new IllegalStateException("Pago rechazado: " + resultado.mensajeError());
        }

        final Inscripcion inscripcion = Inscripcion.builder()
                .usuario(usuario)
                .tipoTicket(tipoTicket)
                .fechaCompra(LocalDateTime.now())
                .culqiChargeId(resultado.referenciaId())
                .build();

        final Inscripcion guardada = inscripcionRepository.save(inscripcion);

        return new InscripcionResponse(
                guardada.getId(),
                torneo.getId(),
                tipoTicket.getTipo(),
                guardada.getFechaCompra()
        );
    }
}