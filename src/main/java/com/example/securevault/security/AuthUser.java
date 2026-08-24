package com.example.securevault.security;

/**
 * Lightweight authenticated principal derived from a validated JWT.
 * Kept minimal on purpose so downstream layers never depend on the JPA entity.
 */
public record AuthUser(Long id, String email) {
}
