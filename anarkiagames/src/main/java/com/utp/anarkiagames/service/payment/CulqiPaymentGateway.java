package com.utp.anarkiagames.service.payment;

import com.culqi.Culqi;
import com.culqi.model.ResponseCulqi;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "payment.gateway", havingValue = "culqi")
public class CulqiPaymentGateway implements PaymentGateway{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PaymentResult cobrar(String tokenId, long montoEnCentimos, String email, String descripcion) throws Exception{
        final Map<String, Object> chargeParams = Map.of(
                "amount", montoEnCentimos,
                "currency_code", "PEN",
                "email", email,
                "source_id", tokenId,
                "description", descripcion,
                "capture", true
        );
        final ResponseCulqi response = new Culqi().charge.create(chargeParams);
        final JsonNode body = objectMapper.readTree(response.getBody());

        if(response.getStatusCode() != 201){
            final String mensaje = body.has("user_message") ? body.get("user_message").asText() : "Pago rechazado";
            return new PaymentResult(false, null, mensaje);
        }

        return new PaymentResult(true,body.get("id").asText(), null);
    }
}
