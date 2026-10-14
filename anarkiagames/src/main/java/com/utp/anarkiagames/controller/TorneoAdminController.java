package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.service.InscripcionService;
import com.utp.anarkiagames.service.TorneoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/torneos")
@RequiredArgsConstructor
public class TorneoAdminController {
    private final TorneoService service;
    private final InscripcionService inscripcionService;

    @PostMapping
    public ResponseEntity<TorneoResponse> crear(@Valid @RequestBody final TorneoRequest request){
        final TorneoResponse response = service.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{torneoId}")
    public ResponseEntity<TorneoResponse> actualizar(
            @PathVariable final Long torneoId,
            @Valid @RequestBody final TorneoRequest request){
        return ResponseEntity.ok(service.actualizar(torneoId, request));
    }

    @PostMapping("/{torneoId}/cancelar")
    public ResponseEntity<CancelacionResponse> cancelar(@PathVariable final Long torneoId){
        return ResponseEntity.ok(service.cancelar(torneoId));
    }

    @GetMapping("/{torneoId}/resumen")
    public ResponseEntity<ResumenTorneoResponse> resumen(@PathVariable final Long torneoId){
        return ResponseEntity.ok(service.resumen(torneoId));
    }

    @GetMapping("/{torneoId}/inscripciones")
    public ResponseEntity<PaginaResponse<InscripcionAdminResponse>> listarInscripciones(
            @PathVariable final Long torneoId,
            @PageableDefault(size = 20, sort = "fechaCompra", direction = Sort.Direction.DESC) final Pageable pageable){
        return ResponseEntity.ok(inscripcionService.listarPorTorneo(torneoId, pageable));
    }
}
