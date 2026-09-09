package com.utp.anarkiagames.service;

import com.utp.anarkiagames.controller.TipoTicketResponse;
import com.utp.anarkiagames.model.TipoTicket;
import com.utp.anarkiagames.model.Torneo;
import com.utp.anarkiagames.repository.TipoTicketRepository;
import com.utp.anarkiagames.repository.TorneoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TipoTicketService {
    private final TipoTicketRepository tipoTicketRepository;
    private final TorneoRepository torneoRepository;

    public TipoTicketResponse crear(final long torneoId, final TipoTicketResponse request){
        final Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(()-> new IllegalArgumentException("Torneo no encontrado"));

        tipoTicketRepository.findByTorneoAndTipo(torneo, request.tipo())
                .ifPresent(t -> { throw new IllegalStateException(
                        "Ya existe un tipo de ticket " + request.tipo() + " para este torneo"); });
        final TipoTicket tipoTicket = TipoTicket.builder()
                .torneo(torneo)
                .tipo(request.tipo())
                .stockMaximo(request.stockMaximo())
                .precio(request.precio())
                .descripcion(request.descripcion())
                .imagenUrl(request.imagenUrl())
                .build();

        final TipoTicket savedTipoTicket = tipoTicketRepository.save(tipoTicket);

        return new TipoTicketResponse(
                savedTipoTicket.getId(),
                torneo.getId(),
                savedTipoTicket.getTipo(),
                savedTipoTicket.getStockMaximo(),
                savedTipoTicket.getPrecio(),
                savedTipoTicket.getDescripcion(),
                savedTipoTicket.getImagenUrl()
        );
    }
}
