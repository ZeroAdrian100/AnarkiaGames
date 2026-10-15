package com.utp.anarkiagames.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificacionesListener {
    private final CorreoService correoService;

    /** Solo después del commit: si la compra se deshace, no se envía nada. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmarCompra(final CompraConfirmadaEvent compra) {
        correoService.enviarConfirmacionCompra(compra);
    }
}
