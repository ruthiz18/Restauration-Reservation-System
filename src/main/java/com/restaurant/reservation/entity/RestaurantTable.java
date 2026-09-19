package com.restaurant.reservation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity 2 - RestaurantTable.
 * A physical table that reservations are assigned to.
 */
@Entity
@Table(name = "restaurant_tables",
        uniqueConstraints = @UniqueConstraint(name = "uk_table_number", columnNames = "table_number"))
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Table number is required")
    @Size(max = 10, message = "Table number must not exceed 10 characters")
    @Column(name = "table_number", nullable = false, unique = true, length = 10)
    private String tableNumber;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 20, message = "Capacity must not exceed 20")
    @Column(nullable = false)
    private Integer capacity;

    @NotBlank(message = "Location is required")
    @Size(max = 50, message = "Location must not exceed 50 characters")
    @Column(nullable = false, length = 50)
    private String location;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TableStatus status = TableStatus.AVAILABLE;

    @OneToMany(mappedBy = "restaurantTable", fetch = FetchType.LAZY)
    private List<Reservation> reservations = new ArrayList<>();

    public RestaurantTable() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public TableStatus getStatus() { return status; }
    public void setStatus(TableStatus status) { this.status = status; }

    public List<Reservation> getReservations() { return reservations; }
    public void setReservations(List<Reservation> reservations) { this.reservations = reservations; }
}
