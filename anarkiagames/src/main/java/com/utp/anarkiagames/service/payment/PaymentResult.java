package com.utp.anarkiagames.service.payment;

public record PaymentResult(
        boolean exitoso,
        String referenciaId,
        String mensajeError
) {
}
