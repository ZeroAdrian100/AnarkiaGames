package com.utp.anarkiagames.service;

import com.utp.anarkiagames.controller.LoginRequest;
import com.utp.anarkiagames.controller.RegisterRequest;
import com.utp.anarkiagames.controller.TokenResponse;
import com.utp.anarkiagames.exception.ConflictoException;
import com.utp.anarkiagames.exception.DemasiadosIntentosException;
import com.utp.anarkiagames.model.Role;
import com.utp.anarkiagames.model.User;
import com.utp.anarkiagames.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final IntentosLoginService intentosLoginService;

    public TokenResponse register(RegisterRequest request){
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictoException("Ya existe una cuenta con ese email");
        }
        var user = User.builder()
                .nombre(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CLIENTE)
                .build();
        var savedUser = userRepository.save(user);
        var jwtToken = jwtService.generateToken(savedUser);
        var refreshToken = jwtService.generateRefreshToken(savedUser);
        return new TokenResponse(jwtToken, refreshToken);
    }

    public TokenResponse login(LoginRequest request, String ip){
        final long minutos = intentosLoginService.minutosBloqueado(request.email(), ip);
        if (minutos > 0) {
            throw new DemasiadosIntentosException(
                    "Demasiados intentos fallidos. Intenta de nuevo en " + minutos + " minuto(s)");
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        } catch (AuthenticationException e) {
            intentosLoginService.registrarFallo(request.email(), ip);
            throw new BadCredentialsException("Email o contraseña incorrectos");
        }
        intentosLoginService.registrarExito(request.email(), ip);
        var user = userRepository.findByEmail(request.email())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        return new TokenResponse(jwtToken, refreshToken);
    }

    public TokenResponse refreshToken(final String authHeader){
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            throw new BadCredentialsException("Refresh token inválido");
        }

        final String refreshToken = authHeader.substring(7);
        try {
            final User user = userRepository.findByEmail(jwtService.extractUsername(refreshToken))
                    .filter(u -> jwtService.isRefreshTokenValid(refreshToken, u))
                    .orElseThrow(() -> new BadCredentialsException("Refresh token inválido"));
            final String accessToken = jwtService.generateToken(user);
            return new TokenResponse(accessToken, refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Refresh token inválido o expirado");
        }
    }
}
