package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.service.TorneoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/torneos")
@RequiredArgsConstructor
public class TorneoController {
    private final TorneoService service;

    @GetMapping
    public ResponseEntity<List<TorneoResponse>> listar(){
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{torneoId}")
    public ResponseEntity<TorneoDetalleResponse> obtener(@PathVariable final Long torneoId){
        return ResponseEntity.ok(service.obtener(torneoId));
    }
}