package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.dto.response.InscripcionAdminResponse;
import com.utp.anarkiagames.service.ReembolsoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/inscripciones")
@RequiredArgsConstructor
public class InscripcionAdminController {
    private final ReembolsoService service;

    @PostMapping("/{inscripcionId}/reembolsar")
    public ResponseEntity<InscripcionAdminResponse> reembolsar(@PathVariable final Long inscripcionId){
        return ResponseEntity.ok(service.reembolsar(inscripcionId));
    }
}
