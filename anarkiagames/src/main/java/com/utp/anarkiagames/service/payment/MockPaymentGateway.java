package com.utp.anarkiagames.service.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "payment.gateway", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway{
    public PaymentResult cobrar(String tokenId, long montoEnCentimos, String email, String descripcion){
        final String referenciaFalsa = "chr_mock_" + UUID.randomUUID();
        return new PaymentResult(true, referenciaFalsa, null);
    }
}
