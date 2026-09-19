package com.restaurant.reservation.exception;

/** Thrown when an entity referenced by id does not exist. Mapped to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " with id " + id + " was not found");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
