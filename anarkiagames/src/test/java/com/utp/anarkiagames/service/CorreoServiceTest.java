package com.utp.anarkiagames.service;

import com.utp.anarkiagames.model.TipoParticipacion;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CorreoServiceTest {
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final CorreoService service = new CorreoService(mailSender);

    private final CompraConfirmadaEvent compra = new CompraConfirmadaEvent(
            "ana@mail.com", "Ana", "Copa Anarkia", "Tekken 8", LocalDateTime.of(2026, 11, 20, 18, 0),
            TipoParticipacion.ESPECTADOR, 2, 5000, List.of("aaa111", "bbb222"));

    @Test
    void sinRemitenteNoEnvia() {
        ReflectionTestUtils.setField(service, "remitente", "");
        service.enviarConfirmacionCompra(compra);
        verifyNoInteractions(mailSender);
    }

    @Test
    void confirmacionIncluyeTotalYCodigos() {
        ReflectionTestUtils.setField(service, "remitente", "tienda@gmail.com");
        service.enviarConfirmacionCompra(compra);

        final ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        final SimpleMailMessage mensaje = captor.getValue();
        assertEquals("tienda@gmail.com", mensaje.getFrom());
        assertArrayEquals(new String[]{"ana@mail.com"}, mensaje.getTo());
        assertTrue(mensaje.getText().contains("S/ 50.00"));
        assertTrue(mensaje.getText().contains("aaa111") && mensaje.getText().contains("bbb222"));
        assertTrue(mensaje.getText().contains("20/11/2026 18:00"));
    }

    @Test
    void unFalloDeEnvioNoSePropaga() {
        ReflectionTestUtils.setField(service, "remitente", "tienda@gmail.com");
        doThrow(new MailSendException("smtp caído")).when(mailSender).send(any(SimpleMailMessage.class));
        assertDoesNotThrow(() -> service.enviarRecuperacion("ana@mail.com", "Ana", "http://x/y?token=t", 15));
    }
}
