package com.restaurant.reservation.dto;

import com.restaurant.reservation.entity.ReservationStatus;
import jakarta.validation.constraints.NotNull;

/** Payload for PATCH /api/v1/reservations/{id}/status. */
public class ReservationStatusRequest {

    @NotNull(message = "Status is required")
    private ReservationStatus status;

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
}
