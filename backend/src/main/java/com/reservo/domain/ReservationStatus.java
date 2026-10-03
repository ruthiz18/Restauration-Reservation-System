package com.reservo.domain;

import java.util.EnumSet;
import java.util.Set;

public enum ReservationStatus {
    PENDING, CONFIRMED, SEATED, COMPLETED, CANCELLED, NO_SHOW;

    /** Statuses that hold a table and therefore block overlapping bookings. */
    public static final Set<ReservationStatus> BLOCKING = EnumSet.of(PENDING, CONFIRMED, SEATED);

    public Set<ReservationStatus> nextStates() {
        return switch (this) {
            case PENDING -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> EnumSet.of(SEATED, CANCELLED, NO_SHOW);
            case SEATED -> EnumSet.of(COMPLETED);
            default -> EnumSet.noneOf(ReservationStatus.class);
        };
    }

    public boolean canMoveTo(ReservationStatus target) {
        return nextStates().contains(target);
    }
}
