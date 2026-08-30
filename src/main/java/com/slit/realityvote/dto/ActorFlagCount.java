package com.slit.realityvote.dto;

/**
 * Top-N actors ranked by number of flagged audit events. Used on the
 * Compliance Officer dashboard's suspicious-actors table.
 */
public record ActorFlagCount(
        String actorEmail,
        long flaggedCount
) {}
