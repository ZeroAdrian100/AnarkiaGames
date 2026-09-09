package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.model.User;
import com.utp.anarkiagames.service.InscripcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/torneos")
@RequiredArgsConstructor
public class InscripcionController {
    private final InscripcionService service;

    public ResponseEntity<InscripcionResponse> comprar(
            @PathVariable final Long torneoId,
            @RequestBody final InscripcionRequest request,
            @AuthenticationPrincipal final User usuario){
        final InscripcionResponse response = service.comprar(torneoId, request, usuario);
        return ResponseEntity.ok(response);
    }
}
