package com.reservo.event;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Domain event raised by the reservation service and sent through RabbitMQ.
 * {@code type} doubles as the routing-key suffix: reservation.created, .confirmed, .cancelled, .status, .reminder.
 */
public record ReservationEvent(String type, Long reservationId, String restaurantName, String recipientName,
                               String recipientEmail, String recipientPhone, LocalDate date, LocalTime time,
                               int partySize, String status) {

    public String routingKey() {
        return "reservation." + type;
    }

    public String summary() {
        return "%s: table for %d at %s on %s %s (status %s)".formatted(
                type, partySize, restaurantName, date, time, status);
    }
}
