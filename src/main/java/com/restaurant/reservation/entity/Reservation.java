package com.restaurant.reservation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity 3 - Reservation.
 * Links a Customer to a RestaurantTable for a given date and time.
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "customer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservation_customer"))
    private Customer customer;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "table_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservation_table"))
    private RestaurantTable restaurantTable;

    @NotNull(message = "Reservation date is required")
    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @NotNull(message = "Reservation time is required")
    @Column(name = "reservation_time", nullable = false)
    private LocalTime reservationTime;

    @NotNull(message = "Party size is required")
    @Min(value = 1, message = "Party size must be at least 1")
    @Column(name = "party_size", nullable = false)
    private Integer partySize;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Size(max = 250, message = "Notes must not exceed 250 characters")
    @Column(length = 250)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Reservation() {
    }

    /** Start of the dining slot as a single comparable value. */
    public LocalDateTime getStartsAt() {
        return LocalDateTime.of(reservationDate, reservationTime);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public RestaurantTable getRestaurantTable() { return restaurantTable; }
    public void setRestaurantTable(RestaurantTable restaurantTable) { this.restaurantTable = restaurantTable; }

    public LocalDate getReservationDate() { return reservationDate; }
    public void setReservationDate(LocalDate reservationDate) { this.reservationDate = reservationDate; }

    public LocalTime getReservationTime() { return reservationTime; }
    public void setReservationTime(LocalTime reservationTime) { this.reservationTime = reservationTime; }

    public Integer getPartySize() { return partySize; }
    public void setPartySize(Integer partySize) { this.partySize = partySize; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
