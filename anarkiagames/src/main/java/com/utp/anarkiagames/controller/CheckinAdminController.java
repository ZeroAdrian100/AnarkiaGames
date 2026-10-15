package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/checkin")
@RequiredArgsConstructor
public class CheckinAdminController {
    private final TicketService service;

    @PostMapping("/{codigo}")
    public ResponseEntity<CheckinResponse> checkin(@PathVariable final String codigo){
        return ResponseEntity.ok(service.checkin(codigo));
    }
}
