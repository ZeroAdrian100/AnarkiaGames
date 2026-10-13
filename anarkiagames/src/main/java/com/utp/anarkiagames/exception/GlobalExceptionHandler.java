package com.utp.anarkiagames.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNoEncontrado(RecursoNoEncontradoException ex){
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> handleConflicto(ConflictoException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PagoRechazadoException.class)
    public ResponseEntity<ErrorResponse> handlePagoRechazado(PagoRechazadoException ex) {
        return construirRespuesta(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAutenticacion(AuthenticationException ex) {
        return construirRespuesta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegridad(DataIntegrityViolationException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleCuerpoInvalido(HttpMessageNotReadableException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la petición es inválido");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenerico(Exception ex) {
        // Las excepciones propias de Spring MVC (404 de ruta, 405, etc.) ya traen su status.
        if (ex instanceof org.springframework.web.ErrorResponse errorSpring) {
            return construirRespuesta(errorSpring.getStatusCode(), ex.getMessage());
        }
        log.error("Error no controlado", ex);
        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado");
    }

    private ResponseEntity<ErrorResponse> construirRespuesta(HttpStatusCode status, String mensaje) {
        final HttpStatus resuelto = HttpStatus.resolve(status.value());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                resuelto != null ? resuelto.getReasonPhrase() : "",
                mensaje
        );
        return ResponseEntity.status(status).body(error);
    }
}
