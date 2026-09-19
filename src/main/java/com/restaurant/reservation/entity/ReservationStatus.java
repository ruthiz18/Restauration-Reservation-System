package com.restaurant.reservation.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle status of a reservation.
 *
 * Allowed transitions:
 *   PENDING   -> CONFIRMED, CANCELLED
 *   CONFIRMED -> SEATED, CANCELLED, NO_SHOW
 *   SEATED    -> COMPLETED
 *   COMPLETED / CANCELLED / NO_SHOW are final states.
 */
public enum ReservationStatus {

    PENDING,
    CONFIRMED,
    SEATED,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    /** Statuses that still hold a table, so they block double booking. */
    public static Set<ReservationStatus> activeStatuses() {
        return EnumSet.of(PENDING, CONFIRMED, SEATED);
    }

    public boolean isActive() {
        return activeStatuses().contains(this);
    }

    public boolean canTransitionTo(ReservationStatus target) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == SEATED || target == CANCELLED || target == NO_SHOW;
            case SEATED -> target == COMPLETED;
            case COMPLETED, CANCELLED, NO_SHOW -> false;
        };
    }
}
