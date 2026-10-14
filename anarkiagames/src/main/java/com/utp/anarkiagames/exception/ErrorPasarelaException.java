package com.utp.anarkiagames.exception;

/** La pasarela de pago no pudo completar una operación (por ejemplo, un reembolso). */
public class ErrorPasarelaException extends RuntimeException{
    public ErrorPasarelaException(String mensaje) {
        super(mensaje);
    }
}
