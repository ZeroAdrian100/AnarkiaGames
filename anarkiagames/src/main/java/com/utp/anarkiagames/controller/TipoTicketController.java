package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.service.TipoTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/admin/torneos/{torneoId}/tipos-ticket")
@RequiredArgsConstructor
public class TipoTicketController {
    private final TipoTicketService service;

    @PostMapping
    public ResponseEntity<TipoTicketResponse> crear(
            @PathVariable final Long torneoId,
            @Valid @RequestBody final TipoTicketRequest request){
        final TipoTicketResponse response = service.crear(torneoId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{tipoTicketId}")
    public ResponseEntity<TipoTicketResponse> actualizar(
            @PathVariable final Long torneoId,
            @PathVariable final Long tipoTicketId,
            @Valid @RequestBody final ActualizarTipoTicketRequest request){
        return ResponseEntity.ok(service.actualizar(torneoId, tipoTicketId, request));
    }

    @DeleteMapping("/{tipoTicketId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable final Long torneoId,
            @PathVariable final Long tipoTicketId){
        service.eliminar(torneoId, tipoTicketId);
        return ResponseEntity.noContent().build();
    }
}
