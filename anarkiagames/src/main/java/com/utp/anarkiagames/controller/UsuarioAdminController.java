package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.model.User;
import com.utp.anarkiagames.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
public class UsuarioAdminController {
    private final UsuarioService service;

    @GetMapping
    public ResponseEntity<PaginaResponse<UsuarioResponse>> listar(
            @RequestParam(required = false) final String buscar,
            @PageableDefault(size = 20, sort = "id") final Pageable pageable){
        return ResponseEntity.ok(service.listar(buscar, pageable));
    }

    @PatchMapping("/{usuarioId}/rol")
    public ResponseEntity<UsuarioResponse> cambiarRol(
            @PathVariable final Long usuarioId,
            @Valid @RequestBody final CambiarRolRequest request,
            @AuthenticationPrincipal final User actual){
        return ResponseEntity.ok(service.cambiarRol(usuarioId, request.role(), actual));
    }
}
