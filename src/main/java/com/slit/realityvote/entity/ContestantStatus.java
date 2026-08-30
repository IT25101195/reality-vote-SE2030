package com.slit.realityvote.entity;

/**
 * Contestant competition status, per the team's requirements doc:
 * "Each contestant should have a status such as Active, Eliminated, or
 * Withdrawn... the system should update the status based on competition
 * results, while authorized staff can make manual updates when needed."
 */
public enum ContestantStatus {
    ACTIVE,
    ELIMINATED,
    WITHDRAWN,
    WINNER
}
