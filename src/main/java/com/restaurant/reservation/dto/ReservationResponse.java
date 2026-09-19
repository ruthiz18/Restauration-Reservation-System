package com.restaurant.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.restaurant.reservation.entity.Reservation;
import com.restaurant.reservation.entity.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ReservationResponse {

    private Long id;

    private Long customerId;
    private String customerName;
    private String customerPhone;

    private Long tableId;
    private String tableNumber;
    private Integer tableCapacity;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reservationDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime reservationTime;

    private Integer partySize;
    private ReservationStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReservationResponse from(Reservation r) {
        ReservationResponse dto = new ReservationResponse();
        dto.id = r.getId();
        dto.customerId = r.getCustomer().getId();
        dto.customerName = r.getCustomer().getFullName();
        dto.customerPhone = r.getCustomer().getPhone();
        dto.tableId = r.getRestaurantTable().getId();
        dto.tableNumber = r.getRestaurantTable().getTableNumber();
        dto.tableCapacity = r.getRestaurantTable().getCapacity();
        dto.reservationDate = r.getReservationDate();
        dto.reservationTime = r.getReservationTime();
        dto.partySize = r.getPartySize();
        dto.status = r.getStatus();
        dto.notes = r.getNotes();
        dto.createdAt = r.getCreatedAt();
        dto.updatedAt = r.getUpdatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public Long getTableId() { return tableId; }
    public String getTableNumber() { return tableNumber; }
    public Integer getTableCapacity() { return tableCapacity; }
    public LocalDate getReservationDate() { return reservationDate; }
    public LocalTime getReservationTime() { return reservationTime; }
    public Integer getPartySize() { return partySize; }
    public ReservationStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
