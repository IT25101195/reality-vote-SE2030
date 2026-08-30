package com.slit.realityvote.entity;

/**
 * SCHEDULED -> created with a start/end time, not accepting votes yet
 * OPEN      -> currently accepting votes (Administrator has activated it)
 * CLOSED    -> voting has ended, results are final
 *
 * Matches the requirements doc: "Only authorized administrators should
 * be able to start, pause, extend, or end voting sessions."
 */
public enum VotingSessionStatus {
    SCHEDULED,
    OPEN,
    CLOSED
}
