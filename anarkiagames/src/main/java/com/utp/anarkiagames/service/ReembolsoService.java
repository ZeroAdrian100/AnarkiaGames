package com.utp.anarkiagames.service;

import com.utp.anarkiagames.dto.response.CancelacionResponse;
import com.utp.anarkiagames.dto.response.InscripcionAdminResponse;
import com.utp.anarkiagames.exception.ConflictoException;
import com.utp.anarkiagames.exception.ErrorPasarelaException;
import com.utp.anarkiagames.exception.RecursoNoEncontradoException;
import com.utp.anarkiagames.model.EstadoInscripcion;
import com.utp.anarkiagames.model.Inscripcion;
import com.utp.anarkiagames.model.Torneo;
import com.utp.anarkiagames.repository.InscripcionRepository;
import com.utp.anarkiagames.service.payment.PaymentGateway;
import com.utp.anarkiagames.service.payment.PaymentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReembolsoService {
    private final InscripcionRepository inscripcionRepository;
    private final PaymentGateway paymentGateway;

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
}
