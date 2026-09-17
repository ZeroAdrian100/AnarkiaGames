package com.utp.anarkiagames.service.payment;

public interface PaymentGateway {
    PaymentResult cobrar(String tokenId, long montoEnCentimos, String email, String descripcion) throws Exception;
}