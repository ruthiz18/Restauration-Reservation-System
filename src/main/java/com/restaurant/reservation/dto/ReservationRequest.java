package com.restaurant.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/** Payload used for POST and PUT on /api/v1/reservations. */
public class ReservationRequest {

    @NotNull(message = "Customer id is required")
    private Long customerId;

    @NotNull(message = "Table id is required")
    private Long tableId;

    @NotNull(message = "Reservation date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reservationDate;

    @NotNull(message = "Reservation time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime reservationTime;

    @NotNull(message = "Party size is required")
    @Min(value = 1, message = "Party size must be at least 1")
    private Integer partySize;

    @Size(max = 250, message = "Notes must not exceed 250 characters")
    private String notes;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }

    public LocalDate getReservationDate() { return reservationDate; }
    public void setReservationDate(LocalDate reservationDate) { this.reservationDate = reservationDate; }

    public LocalTime getReservationTime() { return reservationTime; }
    public void setReservationTime(LocalTime reservationTime) { this.reservationTime = reservationTime; }

    public Integer getPartySize() { return partySize; }
    public void setPartySize(Integer partySize) { this.partySize = partySize; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
