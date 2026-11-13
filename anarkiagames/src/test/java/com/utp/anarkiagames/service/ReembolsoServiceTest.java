package com.utp.anarkiagames.service;

import com.utp.anarkiagames.dto.response.CancelacionResponse;
import com.utp.anarkiagames.exception.ConflictoException;
import com.utp.anarkiagames.exception.ErrorPasarelaException;
import com.utp.anarkiagames.model.EstadoInscripcion;
import com.utp.anarkiagames.model.Inscripcion;
import com.utp.anarkiagames.model.Torneo;
import com.utp.anarkiagames.repository.InscripcionRepository;
import com.utp.anarkiagames.service.payment.PaymentGateway;
import com.utp.anarkiagames.service.payment.PaymentResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReembolsoServiceTest {
    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private PaymentGateway paymentGateway;
    @InjectMocks private ReembolsoService service;

    @Test
    void reembolsoFallidoDevuelveLaCompraAPagada() {
        final Inscripcion inscripcion = Inscripcion.builder().id(7L).referenciaPago("chr_7").monto(4000).build();
        when(inscripcionRepository.findById(7L)).thenReturn(Optional.of(inscripcion));
        when(inscripcionRepository.cambiarEstado(7L, EstadoInscripcion.PAGADA, EstadoInscripcion.REEMBOLSADA))
                .thenReturn(1);
        when(paymentGateway.reembolsar("chr_7", 4000L)).thenReturn(new PaymentResult(false, null, "caida"));

        assertThrows(ErrorPasarelaException.class, () -> service.reembolsar(7L));
        verify(inscripcionRepository).cambiarEstado(7L, EstadoInscripcion.REEMBOLSADA, EstadoInscripcion.PAGADA);
        verify(inscripcionRepository, never()).guardarReferenciaReembolso(anyLong(), any());
    }

    @Test
    void noSeReembolsaDosVeces() {
        final Inscripcion inscripcion = Inscripcion.builder().id(8L).referenciaPago("chr_8").monto(2000).build();
        when(inscripcionRepository.findById(8L)).thenReturn(Optional.of(inscripcion));
        when(inscripcionRepository.cambiarEstado(8L, EstadoInscripcion.PAGADA, EstadoInscripcion.REEMBOLSADA))
                .thenReturn(0);

        assertThrows(ConflictoException.class, () -> service.reembolsar(8L));
        verify(paymentGateway, never()).reembolsar(any(), anyLong());
    }

    @Test
    void reembolsarTodasCuentaReembolsadasYPendientes() {
        final Torneo torneo = Torneo.builder().id(1L).build();
        final Inscripcion ok = Inscripcion.builder().id(1L).referenciaPago("chr_1").monto(2000).build();
        final Inscripcion falla = Inscripcion.builder().id(2L).referenciaPago("chr_2").monto(5000).build();
        final Inscripcion yaReembolsada = Inscripcion.builder().id(3L).referenciaPago("chr_3").monto(2000).build();
        when(inscripcionRepository.findByTipoTicketTorneoAndEstado(torneo, EstadoInscripcion.PAGADA))
                .thenReturn(List.of(ok, falla, yaReembolsada));
        when(inscripcionRepository.cambiarEstado(anyLong(), eq(EstadoInscripcion.PAGADA), eq(EstadoInscripcion.REEMBOLSADA)))
                .thenAnswer(inv -> inv.<Long>getArgument(0) == 3L ? 0 : 1);
        when(paymentGateway.reembolsar("chr_1", 2000L)).thenReturn(new PaymentResult(true, "ref_1", null));
        when(paymentGateway.reembolsar("chr_2", 5000L)).thenReturn(new PaymentResult(false, null, "caida"));

        final CancelacionResponse resultado = service.reembolsarTodas(torneo);

        assertEquals(1, resultado.reembolsadas());
        assertEquals(1, resultado.pendientes());
        verify(inscripcionRepository).guardarReferenciaReembolso(1L, "ref_1");
        verify(paymentGateway, never()).reembolsar(eq("chr_3"), anyLong());
    }
}
