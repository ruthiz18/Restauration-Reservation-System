package com.restaurant.reservation.dto;

import com.restaurant.reservation.entity.RestaurantTable;
import com.restaurant.reservation.entity.TableStatus;

public class RestaurantTableResponse {

    private Long id;
    private String tableNumber;
    private Integer capacity;
    private String location;
    private TableStatus status;

    public static RestaurantTableResponse from(RestaurantTable t) {
        RestaurantTableResponse r = new RestaurantTableResponse();
        r.id = t.getId();
        r.tableNumber = t.getTableNumber();
        r.capacity = t.getCapacity();
        r.location = t.getLocation();
        r.status = t.getStatus();
        return r;
    }

    public Long getId() { return id; }
    public String getTableNumber() { return tableNumber; }
    public Integer getCapacity() { return capacity; }
    public String getLocation() { return location; }
    public TableStatus getStatus() { return status; }
}
