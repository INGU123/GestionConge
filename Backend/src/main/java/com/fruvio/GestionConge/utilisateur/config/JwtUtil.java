package com.fruvio.GestionConge.utilisateur.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private static final long EXPIRATION_MS = 1000L * 60 * 60 * 8; // 8 heures

    @PostConstruct
    private void validateSigningKeyAtStartup() {
        getSigningKey();
    }

    private SecretKey getSigningKey() {
        if (secret == null || secret.isBlank() || secret.trim().startsWith("REMPLACER_")
                || "change-me-in-production".equalsIgnoreCase(secret.trim())) {
            throw new IllegalStateException(
                    "JWT_SECRET doit être défini avec une valeur privée aléatoire d'au moins 32 octets dans .env.");
        }

        byte[] keyBytes = secret.trim().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET doit contenir au moins 32 octets UTF-8. Aucune clé de secours n'est autorisée.");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String matricule, String role) {
        if (matricule == null || matricule.trim().isEmpty()) {
            throw new IllegalArgumentException("Le matricule est obligatoire pour générer un token.");
        }

        return Jwts.builder()
                .subject(matricule.trim())
                .claim("role", role != null ? role : "EMPLOYE")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractMatricule(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims != null ? claims.getSubject() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isTokenValid(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            Claims claims = parseClaims(token);
            if (claims == null || claims.getSubject() == null) {
                return false;
            }
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token.trim())
                .getPayload();
    }
}