package com.example.securevault.config;

/**
 * Central Redis / Spring Cache region names used across services.
 */
public final class CacheNames {

    public static final String USERS = "users";
    public static final String CREDENTIALS = "credentials";
    public static final String CATEGORIES = "categories";

    private CacheNames() {
    }
}
