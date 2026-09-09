package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.service.TorneoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/torneos")
@RequiredArgsConstructor
public class TorneoController {
    private final TorneoService service;

    @PostMapping
    public ResponseEntity<TorneoResponse> crear(@RequestBody final TorneoRequest request){
        final TorneoResponse response = service.crear(request);
        return ResponseEntity.ok(response);
    }
}