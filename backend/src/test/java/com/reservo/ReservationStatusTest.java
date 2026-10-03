package com.reservo;

import com.reservo.domain.ReservationStatus;
import org.junit.jupiter.api.Test;

import static com.reservo.domain.ReservationStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class ReservationStatusTest {

    @Test
    void happyPathIsAllowed() {
        assertTrue(PENDING.canMoveTo(CONFIRMED));
        assertTrue(CONFIRMED.canMoveTo(SEATED));
        assertTrue(SEATED.canMoveTo(COMPLETED));
    }

    @Test
    void terminalStatesCannotChange() {
        for (ReservationStatus s : new ReservationStatus[]{COMPLETED, CANCELLED, NO_SHOW}) {
            assertTrue(s.nextStates().isEmpty(), s + " should be terminal");
        }
    }

    @Test
    void cannotSkipStates() {
        assertFalse(PENDING.canMoveTo(SEATED));
        assertFalse(PENDING.canMoveTo(COMPLETED));
        assertFalse(SEATED.canMoveTo(CANCELLED));
    }

    @Test
    void onlyActiveStatusesBlockTables() {
        assertTrue(BLOCKING.contains(PENDING));
        assertTrue(BLOCKING.contains(CONFIRMED));
        assertTrue(BLOCKING.contains(SEATED));
        assertFalse(BLOCKING.contains(CANCELLED));
        assertFalse(BLOCKING.contains(NO_SHOW));
    }
}
