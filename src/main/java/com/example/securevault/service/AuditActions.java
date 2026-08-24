package com.example.securevault.service;

/**
 * Audit action names written for vault mutations.
 * Kept as constants so service code and tests stay aligned.
 */
public final class AuditActions {

    public static final String CREATED = "CREATED";
    public static final String UPDATED = "UPDATED";
    public static final String DELETED = "DELETED";
    public static final String RESTORED = "RESTORED";
    public static final String PERMANENTLY_DELETED = "PERMANENTLY_DELETED";

    public static final String ENTITY_CREDENTIAL = "CREDENTIAL";

    private AuditActions() {
    }
}
