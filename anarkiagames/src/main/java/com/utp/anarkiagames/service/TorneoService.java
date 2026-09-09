package com.utp.anarkiagames.service;

import com.utp.anarkiagames.controller.TipoTicketResponse;
import com.utp.anarkiagames.controller.TorneoDetalleResponse;
import com.utp.anarkiagames.controller.TorneoRequest;
import com.utp.anarkiagames.controller.TorneoResponse;
import com.utp.anarkiagames.model.Torneo;
import com.utp.anarkiagames.repository.TipoTicketRepository;
import com.utp.anarkiagames.repository.TorneoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TorneoService {
    private final TorneoRepository torneoRepository;
    private final TipoTicketRepository tipoTicketRepository;

    public TorneoResponse crear(final TorneoRequest request){
        final Torneo torneo = Torneo.builder()
                .nombre(request.nombre())
                .juego(request.juego())
                .fechaInicio(request.fechaInicio())
                .build();

        final Torneo guardado = torneoRepository.save(torneo);

        return new TorneoResponse(
                guardado.getId(), guardado.getNombre(), guardado.getJuego(), guardado.getFechaInicio());
    }

    public List<TorneoResponse> listar(){
        return torneoRepository.findAll().stream()
                .map(t -> new TorneoResponse(t.getId(), t.getNombre(), t.getJuego(), t.getFechaInicio()))
                .toList();
    }

    public TorneoDetalleResponse obtener(final Long torneoId){
        final Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new IllegalArgumentException("Torneo no encontrado"));

        final List<TipoTicketResponse> tiposTicket = tipoTicketRepository.findByTorneo(torneo).stream()
                .map(tt -> new TipoTicketResponse(
                        tt.getId(), torneo.getId(), tt.getTipo(),
                        tt.getStockMaximo(), tt.getPrecio(), tt.getDescripcion(), tt.getImagenUrl()))
                .toList();

        return new TorneoDetalleResponse(
                torneo.getId(), torneo.getNombre(), torneo.getJuego(), torneo.getFechaInicio(), tiposTicket);
    }
}