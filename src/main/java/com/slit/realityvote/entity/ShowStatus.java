package com.slit.realityvote.entity;

/**
 * Lifecycle status of a Reality Show.
 * UPCOMING  -> created, not yet started
 * ONGOING   -> currently airing / accepting votes
 * COMPLETED -> finished, winner announced
 * ARCHIVED  -> soft-removed from active listings (see RealityShow.archived)
 */
public enum ShowStatus {
    UPCOMING,
    ONGOING,
    COMPLETED,
    ARCHIVED
}
