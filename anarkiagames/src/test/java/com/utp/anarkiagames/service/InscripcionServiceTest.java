package com.utp.anarkiagames.service;

import com.utp.anarkiagames.controller.InscripcionRequest;
import com.utp.anarkiagames.controller.InscripcionResponse;
import com.utp.anarkiagames.exception.ConflictoException;
import com.utp.anarkiagames.exception.ErrorPasarelaException;
import com.utp.anarkiagames.exception.PagoRechazadoException;
import com.utp.anarkiagames.exception.SolicitudInvalidaException;
import com.utp.anarkiagames.model.EstadoInscripcion;
import com.utp.anarkiagames.model.EstadoTorneo;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InscripcionServiceTest {
    private static final long TORNEO_ID = 1L;

    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private TipoTicketRepository tipoTicketRepository;
    @Mock private TorneoRepository torneoRepository;
    @Mock private PaymentGateway paymentGateway;
    @Mock private TicketService ticketService;
    @Mock private org.springframework.context.ApplicationEventPublisher eventos;
    @InjectMocks private InscripcionService service;

    private final User usuario = User.builder().id(10L).email("cliente@test.com").build();
    private Torneo torneo;
    private TipoTicket espectador;
    private TipoTicket competidor;

    @BeforeEach
    void setUp() {
        torneo = Torneo.builder().id(TORNEO_ID).nombre("Copa").fechaInicio(LocalDateTime.now().plusDays(7)).build();
        espectador = TipoTicket.builder().id(1L).torneo(torneo).tipo(TipoParticipacion.ESPECTADOR)
                .stockMaximo(10).precio(2000).build();
        competidor = TipoTicket.builder().id(2L).torneo(torneo).tipo(TipoParticipacion.COMPETIDOR)
                .stockMaximo(4).precio(5000).build();
        lenient().when(torneoRepository.findByIdForUpdate(TORNEO_ID)).thenReturn(Optional.of(torneo));
        lenient().when(tipoTicketRepository.findByTorneoAndTipo(torneo, TipoParticipacion.ESPECTADOR))
                .thenReturn(Optional.of(espectador));
        lenient().when(tipoTicketRepository.findByTorneoAndTipo(torneo, TipoParticipacion.COMPETIDOR))
                .thenReturn(Optional.of(competidor));
        lenient().when(paymentGateway.cobrar(any(), anyLong(), any(), any()))
                .thenReturn(new PaymentResult(true, "chr_1", null));
        lenient().when(inscripcionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private InscripcionRequest pedido(TipoParticipacion tipo, Integer cantidad) {
        return new InscripcionRequest(tipo, cantidad, "tok_ok");
    }

    @Test
    void espectadorCompraVariosTicketsYPagaElTotal() {
        final InscripcionResponse response = service.comprar(TORNEO_ID, pedido(TipoParticipacion.ESPECTADOR, 3), usuario);

        assertEquals(3, response.cantidad());
        assertEquals(6000, response.monto());
        verify(ticketService).generar(argThat(i -> i.getCantidad() == 3));
        verify(eventos).publishEvent(argThat((Object e) ->
                e instanceof CompraConfirmadaEvent c && c.cantidad() == 3 && c.monto() == 6000));
        verify(paymentGateway).cobrar(eq("tok_ok"), eq(6000L), eq(usuario.getEmail()), any());
    }

    @Test
    void cantidadPorDefectoEsUno() {
        assertEquals(1, service.comprar(TORNEO_ID, pedido(TipoParticipacion.ESPECTADOR, null), usuario).cantidad());
    }

    @Test
    void espectadorNoPuedeComprarMasDelMaximoPorCompra() {
        assertThrows(SolicitudInvalidaException.class,
                () -> service.comprar(TORNEO_ID, pedido(TipoParticipacion.ESPECTADOR, 6), usuario));
        verifyNoInteractions(paymentGateway);
    }

    @Test
    void competidorSoloPuedeComprarUnTicket() {
        assertThrows(SolicitudInvalidaException.class,
                () -> service.comprar(TORNEO_ID, pedido(TipoParticipacion.COMPETIDOR, 2), usuario));
    }

    @Test
    void competidorNoPuedeRepetir() {
        when(inscripcionRepository.existsByUsuarioAndTipoTicketAndEstado(usuario, competidor, EstadoInscripcion.PAGADA)).thenReturn(true);
        assertThrows(ConflictoException.class,
                () -> service.comprar(TORNEO_ID, pedido(TipoParticipacion.COMPETIDOR, 1), usuario));
    }

    @Test
    void noSePuedeTenerOtroTipoEnElMismoTorneo() {
        when(inscripcionRepository.existsConOtroTipoEnTorneo(usuario, torneo, TipoParticipacion.COMPETIDOR))
                .thenReturn(true);
        assertThrows(ConflictoException.class,
                () -> service.comprar(TORNEO_ID, pedido(TipoParticipacion.COMPETIDOR, 1), usuario));
        verifyNoInteractions(paymentGateway);
    }

    @Test
    void noSeVendeDespuesDeLaFechaDeInicio() {
        torneo.setFechaInicio(LocalDateTime.now().minusMinutes(1));
        assertThrows(ConflictoException.class,
                () -> service.comprar(TORNEO_ID, pedido(TipoParticipacion.ESPECTADOR, 1), usuario));
    }

    @Test
    void elStockCuentaLaSumaDeCantidades() {
        when(inscripcionRepository.sumCantidadByTipoTicket(espectador)).thenReturn(8L);
        assertThrows(ConflictoException.class,
                () -> service.comprar(TORNEO_ID, pedido(TipoParticipacion.ESPECTADOR, 3), usuario));
        verifyNoInteractions(paymentGateway);
    }

    @Test
    void pagoRechazadoNoGuardaInscripcion() {
        when(paymentGateway.cobrar(any(), anyLong(), any(), any()))
                .thenReturn(new PaymentResult(false, null, "Tarjeta rechazada"));
        assertThrows(PagoRechazadoException.class,
                () -> service.comprar(TORNEO_ID, pedido(TipoParticipacion.ESPECTADOR, 1), usuario));
        verify(inscripcionRepository, never()).saveAndFlush(any(Inscripcion.class));
        verifyNoInteractions(eventos);
    }

    @Test
    void siFallaElGuardadoSeReembolsaElCargo() {
        when(inscripcionRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));
        when(paymentGateway.reembolsar(any(), anyLong())).thenReturn(new PaymentResult(true, "ref_1", null));

        assertThrows(DataIntegrityViolationException.class,
                () -> service.comprar(TORNEO_ID, pedido(TipoParticipacion.ESPECTADOR, 2), usuario));
        verify(paymentGateway).reembolsar("chr_1", 4000L);
    }

    @Test
    void noSeVendeEnUnTorneoCancelado() {
        torneo.setEstado(EstadoTorneo.CANCELADO);
        assertThrows(ConflictoException.class,
                () -> service.comprar(TORNEO_ID, pedido(TipoParticipacion.ESPECTADOR, 1), usuario));
        verifyNoInteractions(paymentGateway);
    }

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
    void siFallaLaGeneracionDeTicketsSeReembolsaElCargo() {
        doThrow(new IllegalStateException("codigo duplicado")).when(ticketService).generar(any());
        when(paymentGateway.reembolsar(any(), anyLong())).thenReturn(new PaymentResult(true, "ref_2", null));

        assertThrows(IllegalStateException.class,
                () -> service.comprar(TORNEO_ID, pedido(TipoParticipacion.ESPECTADOR, 1), usuario));
        verify(paymentGateway).reembolsar("chr_1", 2000L);
    }
}
