package com.utp.anarkiagames.service.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Pasarela simulada para desarrollo y demos sin credenciales reales.
 * Todo cobro es exitoso, salvo que el token sea {@value #TOKEN_RECHAZADO}.
 */
@Component
@ConditionalOnProperty(name = "payment.gateway", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway{
    public static final String TOKEN_RECHAZADO = "tok_rechazado";

    @Override
    public PaymentResult cobrar(String tokenId, long montoEnCentimos, String email, String descripcion){
        if (TOKEN_RECHAZADO.equals(tokenId)) {
            return new PaymentResult(false, null, "Tarjeta rechazada (simulado)");
        }
        return new PaymentResult(true, "chr_mock_" + UUID.randomUUID(), null);
    }

    @Override
    public PaymentResult reembolsar(String referenciaId, long montoEnCentimos){
        return new PaymentResult(true, "ref_mock_" + UUID.randomUUID(), null);
    }
}
