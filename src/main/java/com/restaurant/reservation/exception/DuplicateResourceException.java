package com.restaurant.reservation.exception;

/** Thrown when a uniqueness rule is violated (BR-02). Mapped to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
