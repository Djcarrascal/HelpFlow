package com.helpflow.support.infrastructure.security;

import com.helpflow.support.application.port.out.TokenPort;
import com.helpflow.support.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtService implements TokenPort {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${helpflow.jwt.secret}") String secret,
                      @Value("${helpflow.jwt.expiration-minutes}") long expirationMinutes) {
        // Clave HMAC-SHA256 a partir del secreto Base64
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMinutes = expirationMinutes;
    }

    @Override
    public String generar(User user) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(user.getCorreo())
                .claim("uid", user.getId())
                .claim("rol", user.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    @Override
    public long expiracionSegundos() {
        return expirationMinutes * 60;
    }

    // Lanza JwtException si la firma no coincide o el token expiró
    public Claims validar(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
