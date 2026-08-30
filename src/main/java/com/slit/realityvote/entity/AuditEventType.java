package com.slit.realityvote.entity;

/**
 * What kind of thing happened. Kept as a flat enum (rather than free text)
 * so the Compliance Officer can filter the audit log reliably - matches
 * "What information should be recorded in audit logs?" from the
 * requirements doc: voter/contestant/session id, timestamp, status,
 * login activity, admin actions, security incidents/alerts.
 */
public enum AuditEventType {
    VOTE_CAST,
    VOTE_REJECTED,
    SESSION_OPENED,
    SESSION_CLOSED,
    SESSION_DELETED,
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    USER_REGISTERED,
    PROFILE_UPDATED,
    PASSWORD_CHANGED,
    SUSPICIOUS_ACTIVITY
}
