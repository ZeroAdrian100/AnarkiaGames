package com.utp.anarkiagames.controller;

import com.utp.anarkiagames.service.AuthService;
import com.utp.anarkiagames.service.RecuperacionPasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;
    private final RecuperacionPasswordService recuperacionService;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody final RegisterRequest request){
        final TokenResponse token = service.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody final LoginRequest request,
                                               final HttpServletRequest httpRequest){
        final TokenResponse token = service.login(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(token);
    }

    @PostMapping("/refresh")
    public TokenResponse refreshToken(@RequestHeader(HttpHeaders.AUTHORIZATION) final String authHeader){
        return service.refreshToken(authHeader);
    }

    @PostMapping("/olvide-password")
    public ResponseEntity<MensajeResponse> olvidePassword(@Valid @RequestBody final RecuperarPasswordRequest request){
        recuperacionService.solicitar(request.email());
        return ResponseEntity.ok(new MensajeResponse(
                "Si el email está registrado, te enviamos un enlace para restablecer la contraseña"));
    }

    @PostMapping("/restablecer-password")
    public ResponseEntity<MensajeResponse> restablecerPassword(@Valid @RequestBody final RestablecerPasswordRequest request){
        recuperacionService.restablecer(request.token(), request.password());
        return ResponseEntity.ok(new MensajeResponse("Contraseña actualizada"));
    }
}
