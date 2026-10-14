package com.utp.anarkiagames.service;

import com.utp.anarkiagames.controller.ActualizarTipoTicketRequest;
import com.utp.anarkiagames.controller.TipoTicketRequest;
import com.utp.anarkiagames.controller.TipoTicketResponse;
import com.utp.anarkiagames.exception.ConflictoException;
import com.utp.anarkiagames.exception.RecursoNoEncontradoException;
import com.utp.anarkiagames.model.TipoTicket;
import com.utp.anarkiagames.model.Torneo;
import com.utp.anarkiagames.repository.InscripcionRepository;
import com.utp.anarkiagames.repository.TipoTicketRepository;
import com.utp.anarkiagames.repository.TorneoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TipoTicketService {
    private final TipoTicketRepository tipoTicketRepository;
    private final TorneoRepository torneoRepository;
    private final InscripcionRepository inscripcionRepository;

    public TipoTicketResponse crear(final Long torneoId, final TipoTicketRequest request){
        final Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(()-> new RecursoNoEncontradoException("Torneo no encontrado"));

        tipoTicketRepository.findByTorneoAndTipo(torneo, request.tipo())
                .ifPresent(t -> { throw new ConflictoException(
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

        return TipoTicketResponse.of(savedTipoTicket, 0);
    }

    /**
     * Bloquea el torneo como una compra, así el stock no baja de lo vendido
     * mientras se procesa una compra simultánea.
     */
    @Transactional
    public TipoTicketResponse actualizar(final Long torneoId, final Long tipoTicketId,
                                         final ActualizarTipoTicketRequest request){
        torneoRepository.findByIdForUpdate(torneoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Torneo no encontrado"));
        final TipoTicket tipoTicket = buscar(torneoId, tipoTicketId);

        final long vendidos = inscripcionRepository.sumCantidadByTipoTicket(tipoTicket);
        if (request.stockMaximo() < vendidos) {
            throw new ConflictoException("El stock no puede ser menor a los " + vendidos + " tickets ya vendidos");
        }
        tipoTicket.setStockMaximo(request.stockMaximo());
        tipoTicket.setPrecio(request.precio());
        tipoTicket.setDescripcion(request.descripcion());
        tipoTicket.setImagenUrl(request.imagenUrl());
        return TipoTicketResponse.of(tipoTicket, vendidos);
    }

    /** Solo se puede eliminar un tipo de ticket sin ventas (ni siquiera reembolsadas, para conservar el historial). */
    @Transactional
    public void eliminar(final Long torneoId, final Long tipoTicketId){
        final TipoTicket tipoTicket = buscar(torneoId, tipoTicketId);
        if (inscripcionRepository.existsByTipoTicket(tipoTicket)) {
            throw new ConflictoException("No se puede eliminar un tipo de ticket que ya tiene ventas");
        }
        tipoTicketRepository.delete(tipoTicket);
    }

    private TipoTicket buscar(final Long torneoId, final Long tipoTicketId){
        return tipoTicketRepository.findById(tipoTicketId)
                .filter(tt -> tt.getTorneo().getId().equals(torneoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de ticket no encontrado en este torneo"));
    }
}
