package com.utp.anarkiagames.service.payment;

import com.culqi.Culqi;
import com.culqi.model.ResponseCulqi;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "payment.gateway", havingValue = "culqi")
public class CulqiPaymentGateway implements PaymentGateway{
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CulqiPaymentGateway(@Value("${culqi.secret_key}") final String secretKey){
        Culqi.secret_key = secretKey;
    }

    @Override
    public PaymentResult cobrar(String tokenId, long montoEnCentimos, String email, String descripcion){
        if (tokenId == null || tokenId.isBlank()) {
            return new PaymentResult(false, null, "Falta el token de la tarjeta");
        }
        final Map<String, Object> chargeParams = Map.of(
                "amount", montoEnCentimos,
                "currency_code", "PEN",
                "email", email,
                "source_id", tokenId,
                "description", descripcion,
                "capture", true
        );
        try {
            return procesar(new Culqi().charge.create(chargeParams), "Pago rechazado");
        } catch (Exception e) {
            log.error("Error al cobrar con Culqi", e);
            return new PaymentResult(false, null, "No se pudo comunicar con la pasarela de pagos");
        }
    }

    @Override
    public PaymentResult reembolsar(String referenciaId, long montoEnCentimos){
        final Map<String, Object> refundParams = Map.of(
                "amount", montoEnCentimos,
                "charge_id", referenciaId,
                "reason", "solicitud_comprador"
        );
        try {
            return procesar(new Culqi().refund.create(refundParams), "Reembolso rechazado");
        } catch (Exception e) {
            log.error("Error al reembolsar el cargo {} con Culqi", referenciaId, e);
            return new PaymentResult(false, null, "No se pudo comunicar con la pasarela de pagos");
        }
    }

    private PaymentResult procesar(final ResponseCulqi response, final String mensajePorDefecto) throws Exception{
        final JsonNode body = objectMapper.readTree(response.getBody());
        if (response.getStatusCode() != 201) {
            final String mensaje = body.has("user_message") ? body.get("user_message").asText() : mensajePorDefecto;
            return new PaymentResult(false, null, mensaje);
        }
        return new PaymentResult(true, body.get("id").asText(), null);
    }
}
