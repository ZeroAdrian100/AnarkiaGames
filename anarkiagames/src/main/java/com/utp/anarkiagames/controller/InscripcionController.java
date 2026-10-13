package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.model.User;
import com.utp.anarkiagames.service.InscripcionService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/torneos")
@RequiredArgsConstructor
public class InscripcionController {
    private final InscripcionService service;

    @PostMapping("/{torneoId}/inscripciones")
    public ResponseEntity<InscripcionResponse> comprar(
            @PathVariable final Long torneoId,
            @Valid @RequestBody final InscripcionRequest request,
            @AuthenticationPrincipal final User usuario){
        final InscripcionResponse response = service.comprar(torneoId, request, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping("/mis-inscripciones")
    public ResponseEntity<List<MiInscripcionResponse>> listarMisInscripciones(@AuthenticationPrincipal final User usuario){
        return ResponseEntity.ok(service.listarMisInscripciones(usuario));
    }
}
