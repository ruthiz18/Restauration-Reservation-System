package com.restaurant.reservation.dto;

import com.restaurant.reservation.entity.Customer;

import java.time.LocalDateTime;

public class CustomerResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CustomerResponse from(Customer c) {
        CustomerResponse r = new CustomerResponse();
        r.id = c.getId();
        r.firstName = c.getFirstName();
        r.lastName = c.getLastName();
        r.fullName = c.getFullName();
        r.email = c.getEmail();
        r.phone = c.getPhone();
        r.address = c.getAddress();
        r.createdAt = c.getCreatedAt();
        r.updatedAt = c.getUpdatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
