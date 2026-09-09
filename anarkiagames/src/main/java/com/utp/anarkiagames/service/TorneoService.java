package com.utp.anarkiagames.service;

import com.utp.anarkiagames.controller.TorneoRequest;
import com.utp.anarkiagames.controller.TorneoResponse;
import com.utp.anarkiagames.model.Torneo;
import com.utp.anarkiagames.repository.TorneoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TorneoService {
    private final TorneoRepository torneoRepository;

    public TorneoResponse crear(final TorneoRequest request){
        final Torneo torneo = Torneo.builder()
                .nombre(request.nombre())
                .juego(request.juego())
                .fechaInicio(request.fechaInicio())
                .build();

        final Torneo savedTorneo = torneoRepository.save(torneo);

        return new TorneoResponse(
                savedTorneo.getId(),
                savedTorneo.getNombre(),
                savedTorneo.getJuego(),
                savedTorneo.getFechaInicio());
    }
}
