package com.slit.realityvote.entity;

/**
 * Lifecycle of a SecurityAlert.
 * Transitions are one-way: OPEN → INVESTIGATING → RESOLVED.
 * A RESOLVED alert cannot be deleted or set back to OPEN — it stays as evidence.
 */
public enum AlertStatus {
    OPEN,
    INVESTIGATING,
    RESOLVED
}
