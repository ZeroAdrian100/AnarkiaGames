package com.utp.anarkiagames.service;

import com.utp.anarkiagames.controller.CancelacionResponse;
import com.utp.anarkiagames.controller.ResumenTorneoResponse;
import com.utp.anarkiagames.controller.TipoTicketResponse;
import com.utp.anarkiagames.controller.TorneoDetalleResponse;
import com.utp.anarkiagames.controller.TorneoRequest;
import com.utp.anarkiagames.controller.TorneoResponse;
import com.utp.anarkiagames.exception.ConflictoException;
import com.utp.anarkiagames.exception.RecursoNoEncontradoException;
import com.utp.anarkiagames.model.EstadoTorneo;
import com.utp.anarkiagames.model.Torneo;
import com.utp.anarkiagames.repository.InscripcionRepository;
import com.utp.anarkiagames.repository.TipoTicketRepository;
import com.utp.anarkiagames.repository.TorneoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TorneoService {
    private final TorneoRepository torneoRepository;
    private final TipoTicketRepository tipoTicketRepository;
    private final InscripcionRepository inscripcionRepository;
    private final InscripcionService inscripcionService;

    public TorneoResponse crear(final TorneoRequest request){
        final Torneo torneo = Torneo.builder()
                .nombre(request.nombre())
                .juego(request.juego())
                .fechaInicio(request.fechaInicio())
                .build();

        return TorneoResponse.of(torneoRepository.save(torneo));
    }

    public List<TorneoResponse> listar(){
        return torneoRepository.findAll().stream()
                .map(TorneoResponse::of)
                .toList();
    }

    public TorneoDetalleResponse obtener(final Long torneoId){
        final Torneo torneo = buscar(torneoId);

        final List<TipoTicketResponse> tiposTicket = tipoTicketRepository.findByTorneo(torneo).stream()
                .map(tt -> TipoTicketResponse.of(tt, inscripcionRepository.sumCantidadByTipoTicket(tt)))
                .toList();

        return new TorneoDetalleResponse(
                torneo.getId(), torneo.getNombre(), torneo.getJuego(), torneo.getFechaInicio(), torneo.getEstado(),
                tiposTicket);
    }

    @Transactional
    public TorneoResponse actualizar(final Long torneoId, final TorneoRequest request){
        final Torneo torneo = buscar(torneoId);
        if (torneo.getEstado() == EstadoTorneo.CANCELADO) {
            throw new ConflictoException("No se puede editar un torneo cancelado");
        }
        torneo.setNombre(request.nombre());
        torneo.setJuego(request.juego());
        torneo.setFechaInicio(request.fechaInicio());
        return TorneoResponse.of(torneo);
    }

    /**
     * Cancela el torneo (cierra las ventas) y reembolsa todas sus compras pagadas.
     * Si algún reembolso falla, se puede volver a llamar: solo se reintentan las compras pendientes.
     */
    public CancelacionResponse cancelar(final Long torneoId){
        final Torneo torneo = buscar(torneoId);
        torneoRepository.marcarCancelado(torneoId);
        return inscripcionService.reembolsarTodas(torneo);
    }

    public ResumenTorneoResponse resumen(final Long torneoId){
        final Torneo torneo = buscar(torneoId);

        final List<ResumenTorneoResponse.TipoTicketResumen> tipos = tipoTicketRepository.findByTorneo(torneo).stream()
                .map(tt -> {
                    final long vendidos = inscripcionRepository.sumCantidadByTipoTicket(tt);
                    return new ResumenTorneoResponse.TipoTicketResumen(
                            tt.getId(), tt.getTipo(), tt.getPrecio(), tt.getStockMaximo(),
                            vendidos, tt.getStockMaximo() - vendidos, inscripcionRepository.sumMontoByTipoTicket(tt));
                })
                .toList();

        final long ingresosTotales = tipos.stream().mapToLong(ResumenTorneoResponse.TipoTicketResumen::ingresos).sum();
        return new ResumenTorneoResponse(torneo.getId(), torneo.getNombre(), torneo.getEstado(), tipos, ingresosTotales);
    }

    private Torneo buscar(final Long torneoId){
        return torneoRepository.findById(torneoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Torneo no encontrado"));
    }
}
