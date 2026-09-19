package com.restaurant.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Restaurant Reservation System - Assignment 2.
 * REST API exposing full CRUD for Customer, RestaurantTable and Reservation.
 */
@SpringBootApplication
public class RestaurantReservationApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantReservationApiApplication.class, args);
    }
}
