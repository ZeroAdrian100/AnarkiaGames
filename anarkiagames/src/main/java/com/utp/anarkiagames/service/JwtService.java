package com.utp.anarkiagames.service;

import com.utp.anarkiagames.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * Emite y valida los JWT. Cada token lleva el claim {@code type} ("access" o "refresh")
 * para que un refresh token no sirva como access token ni al revés.
 * Los métodos de lectura lanzan {@link io.jsonwebtoken.JwtException} si el token es inválido o expiró.
 */
@Service
public class JwtService {
    private static final String CLAIM_TIPO = "type";
    private static final String TIPO_ACCESS = "access";
    private static final String TIPO_REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String secretKey;
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    @Value("${jwt.refresh.expiration}")
    private long refreshExpiration;

    public String extractUsername(final String token){
        return parseClaims(token).getSubject();
    }
    public String generateToken(final User user){
        return buildToken(user, jwtExpiration, TIPO_ACCESS);
    }
    public String generateRefreshToken(final User user){
        return buildToken(user, refreshExpiration, TIPO_REFRESH);
    }
    public boolean isAccessTokenValid(final String token, final User user){
        return isTokenValid(token, user, TIPO_ACCESS);
    }
    public boolean isRefreshTokenValid(final String token, final User user){
        return isTokenValid(token, user, TIPO_REFRESH);
    }

    private String buildToken(final User user, final long expiration, final String tipo){
        return Jwts.builder()
                .id(user.getId().toString())
                .claims(Map.of(
                        "name", user.getNombre(),
                        "role", user.getRole().name(),
                        CLAIM_TIPO, tipo
                ))
                .subject(user.getEmail())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis()+expiration))
                .signWith(getSignInKey())
                .compact()
                ;
    }

    // El parser ya rechaza tokens expirados, así que solo falta comparar dueño y tipo.
    private boolean isTokenValid(final String token, final User user, final String tipo){
        final Claims claims = parseClaims(token);
        return user.getEmail().equals(claims.getSubject()) && tipo.equals(claims.get(CLAIM_TIPO));
    }

    private Claims parseClaims(final String token){
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey(){
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
