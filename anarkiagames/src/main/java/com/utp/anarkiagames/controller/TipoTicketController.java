package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.service.TipoTicketService;
import lombok.RequiredArgsConstructor;
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
            @RequestBody final TipoTicketRequest request){
        final TipoTicketResponse response = service.crear(torneoId, request);
        return ResponseEntity.ok(response);
    }
}