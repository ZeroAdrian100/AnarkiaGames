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

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    @Value("${jwt.refresh.expiration}")
    private long refreshExpiration;

    public String extractUsername(final String token){
        final Claims jwtToken = Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return jwtToken.getSubject();
    }
    public String generateToken(final User user){
        return buildToken(user, jwtExpiration);
    }
    public String generateRefreshToken(final User user){
        return buildToken(user, refreshExpiration);
    }

    private String buildToken(final User user, final long expiration){
        return Jwts.builder()
                .id(user.getID().toString())
                .claims(Map.of(
                        "name", user.getNombre(),
                        "role", user.getRole().name()
                ))
                .subject(user.getEmail())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis()+expiration))
                .signWith(getSignInKey())
                .compact()
                ;
    }

    private SecretKey getSignInKey(){
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean isTokenValid(final String token, final User user){
        final String username = extractUsername(token);
        return (username.equals(user.getEmail()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(final String token){
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(final String token){
        final Claims jwtToken = Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return jwtToken.getExpiration();
    }
}
