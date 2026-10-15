package com.utp.anarkiagames.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Envía los correos en otro hilo. Un fallo de envío queda en el log y no afecta a la operación que lo pidió.
 * Sin MAIL_USERNAME el correo se escribe en el log en lugar de enviarse (útil en desarrollo).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CorreoService {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String remitente;

    @Async
    public void enviarConfirmacionCompra(final CompraConfirmadaEvent compra) {
        final String codigos = compra.codigos().stream()
                .map(codigo -> "  - " + codigo)
                .collect(Collectors.joining(System.lineSeparator()));
        enviar(compra.email(), "Tu compra para " + compra.torneo() + " está confirmada", String.join(System.lineSeparator(),
                "Hola " + compra.nombre() + ",",
                "",
                "Tu compra para " + compra.torneo() + " (" + compra.juego() + ") está confirmada.",
                "Fecha del torneo: " + compra.fechaInicio().format(FECHA),
                "Tipo: " + compra.tipo() + " | Cantidad: " + compra.cantidad() + " | Total: " + soles(compra.monto()),
                "",
                "Tus códigos de entrada (cada uno es un QR que se escanea en la puerta):",
                codigos,
                "",
                "AnarkiaGames"));
    }

    @Async
    public void enviarRecuperacion(final String email, final String nombre, final String enlace, final long minutos) {
        enviar(email, "Restablece tu contraseña de AnarkiaGames", String.join(System.lineSeparator(),
                "Hola " + nombre + ",",
                "",
                "Recibimos un pedido para restablecer tu contraseña. Usa este enlace; vence en " + minutos + " minutos:",
                enlace,
                "",
                "Si no fuiste tú, ignora este correo: tu contraseña no cambia.",
                "",
                "AnarkiaGames"));
    }

    void enviar(final String para, final String asunto, final String texto) {
        if (remitente.isBlank()) {
            log.info("Correo no enviado (MAIL_USERNAME sin configurar). Para: {} | Asunto: {}{}{}",
                    para, asunto, System.lineSeparator(), texto);
            return;
        }
        final SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(para);
        mensaje.setSubject(asunto);
        mensaje.setText(texto);
        try {
            mailSender.send(mensaje);
        } catch (MailException e) {
            log.error("No se pudo enviar el correo '{}' a {}", asunto, para, e);
        }
    }

    private static String soles(final long centimos) {
        return String.format(Locale.ROOT, "S/ %.2f", centimos / 100.0);
    }
}
