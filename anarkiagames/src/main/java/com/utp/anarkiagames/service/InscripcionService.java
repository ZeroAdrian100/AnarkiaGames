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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InscripcionService {
    private final InscripcionRepository inscripcionRepository;
    private final TipoTicketRepository tipoTicketRepository;
    private final TorneoRepository torneoRepository;

    public InscripcionResponse comprar(final long torneoId, final InscripcionRequest request, final User usuario){
        final Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(()-> new IllegalArgumentException("Torneo no encontrado"));

        final TipoTicket tipoTicket = tipoTicketRepository.findByTorneoAndTipo(torneo, request.tipo())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay tickets de tipo " + request.tipo() + " configurados para este torneo"));

        inscripcionRepository.findByUsuarioAndTipoTicket(usuario, tipoTicket)
                .ifPresent(i -> { throw new IllegalStateException("Ya tienes un ticket de este tipo para este torneo"); });

        final Inscripcion inscripcion = Inscripcion.builder()
                .usuario(usuario)
                .tipoTicket(tipoTicket)
                .fechaCompra(LocalDateTime.now())
                .build();

        final Inscripcion savedinscripcion = inscripcionRepository.save(inscripcion);

        return new InscripcionResponse(
                savedinscripcion.getId(),
                torneo.getId(),
                tipoTicket.getTipo(),
                savedinscripcion.getFechaCompra()
        );
    }
}
