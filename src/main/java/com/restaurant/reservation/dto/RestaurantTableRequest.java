package com.restaurant.reservation.dto;

import com.restaurant.reservation.entity.TableStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Payload used for POST and PUT on /api/v1/tables. */
public class RestaurantTableRequest {

    @NotBlank(message = "Table number is required")
    @Size(max = 10, message = "Table number must not exceed 10 characters")
    private String tableNumber;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 20, message = "Capacity must not exceed 20")
    private Integer capacity;

    @NotBlank(message = "Location is required")
    @Size(max = 50, message = "Location must not exceed 50 characters")
    private String location;

    /** Optional on create; defaults to AVAILABLE. */
    private TableStatus status;

    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public TableStatus getStatus() { return status; }
    public void setStatus(TableStatus status) { this.status = status; }
}
