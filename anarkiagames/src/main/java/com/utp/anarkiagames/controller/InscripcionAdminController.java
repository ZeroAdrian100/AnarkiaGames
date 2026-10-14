package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.service.InscripcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/inscripciones")
@RequiredArgsConstructor
public class InscripcionAdminController {
    private final InscripcionService service;

    @PostMapping("/{inscripcionId}/reembolsar")
    public ResponseEntity<InscripcionAdminResponse> reembolsar(@PathVariable final Long inscripcionId){
        return ResponseEntity.ok(service.reembolsar(inscripcionId));
    }
}
