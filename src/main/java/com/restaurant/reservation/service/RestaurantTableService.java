package com.restaurant.reservation.service;

import com.restaurant.reservation.dto.RestaurantTableRequest;
import com.restaurant.reservation.dto.RestaurantTableResponse;
import com.restaurant.reservation.entity.ReservationStatus;
import com.restaurant.reservation.entity.RestaurantTable;
import com.restaurant.reservation.entity.TableStatus;
import com.restaurant.reservation.exception.BusinessRuleException;
import com.restaurant.reservation.exception.DuplicateResourceException;
import com.restaurant.reservation.exception.ResourceNotFoundException;
import com.restaurant.reservation.repository.ReservationRepository;
import com.restaurant.reservation.repository.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for RestaurantTable.
 *
 * Rules enforced here:
 *  - table numbers are unique across the restaurant
 *  - capacity must be between 1 and 20 seats
 *  - a table cannot be marked OUT_OF_SERVICE while it still holds active reservations
 *  - a table with active reservations cannot be deleted
 */
@Service
@Transactional
public class RestaurantTableService {

    private final RestaurantTableRepository tableRepository;
    private final ReservationRepository reservationRepository;

    public RestaurantTableService(RestaurantTableRepository tableRepository,
                                  ReservationRepository reservationRepository) {
        this.tableRepository = tableRepository;
        this.reservationRepository = reservationRepository;
    }

    /** CREATE. */
    public RestaurantTableResponse create(RestaurantTableRequest request) {
        String number = request.getTableNumber().trim();

        if (tableRepository.existsByTableNumberIgnoreCase(number)) {
            throw new DuplicateResourceException("Table number " + number + " already exists");
        }

        RestaurantTable table = new RestaurantTable();
        table.setTableNumber(number);
        table.setCapacity(request.getCapacity());
        table.setLocation(request.getLocation().trim());
        table.setStatus(request.getStatus() == null ? TableStatus.AVAILABLE : request.getStatus());

        return RestaurantTableResponse.from(tableRepository.save(table));
    }

    /** READ all. */
    @Transactional(readOnly = true)
    public List<RestaurantTableResponse> findAll() {
        return tableRepository.findAll().stream()
                .map(RestaurantTableResponse::from)
                .toList();
    }

    /** READ one. */
    @Transactional(readOnly = true)
    public RestaurantTableResponse findById(Long id) {
        return RestaurantTableResponse.from(getEntity(id));
    }

    /** READ filtered by status, minimum capacity or keyword. */
    @Transactional(readOnly = true)
    public List<RestaurantTableResponse> find(TableStatus status, Integer minCapacity, String keyword) {
        List<RestaurantTable> result;

        if (keyword != null && !keyword.isBlank()) {
            result = tableRepository.search(keyword.trim());
        } else if (status != null) {
            result = tableRepository.findByStatus(status);
        } else if (minCapacity != null) {
            result = tableRepository.findByCapacityGreaterThanEqualOrderByCapacityAsc(minCapacity);
        } else {
            result = tableRepository.findAll();
        }

        return result.stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> minCapacity == null || t.getCapacity() >= minCapacity)
                .map(RestaurantTableResponse::from)
                .toList();
    }

    /** UPDATE. */
    public RestaurantTableResponse update(Long id, RestaurantTableRequest request) {
        RestaurantTable table = getEntity(id);
        String number = request.getTableNumber().trim();

        boolean numberChanged = !table.getTableNumber().equalsIgnoreCase(number);
        if (numberChanged && tableRepository.existsByTableNumberIgnoreCase(number)) {
            throw new DuplicateResourceException("Table number " + number + " already exists");
        }

        TableStatus newStatus = request.getStatus() == null ? table.getStatus() : request.getStatus();

        // Taking a table out of service would strand any booking already made on it.
        if (newStatus == TableStatus.OUT_OF_SERVICE && hasActiveReservations(id)) {
            throw new BusinessRuleException(
                    "Table " + table.getTableNumber() + " has active reservations and cannot be "
                            + "marked OUT_OF_SERVICE. Move or cancel those reservations first.");
        }

        // Shrinking a table below the size of a party already booked on it is not allowed.
        if (request.getCapacity() < table.getCapacity()) {
            int largestBookedParty = reservationRepository
                    .findByRestaurantTableIdOrderByReservationDateDescReservationTimeDesc(id).stream()
                    .filter(r -> r.getStatus().isActive())
                    .mapToInt(r -> r.getPartySize())
                    .max()
                    .orElse(0);

            if (request.getCapacity() < largestBookedParty) {
                throw new BusinessRuleException(
                        "Capacity cannot be reduced to " + request.getCapacity()
                                + " because an active reservation on this table is for "
                                + largestBookedParty + " guests.");
            }
        }

        table.setTableNumber(number);
        table.setCapacity(request.getCapacity());
        table.setLocation(request.getLocation().trim());
        table.setStatus(newStatus);

        return RestaurantTableResponse.from(tableRepository.save(table));
    }

    /** DELETE. */
    public void delete(Long id) {
        RestaurantTable table = getEntity(id);

        if (hasActiveReservations(id)) {
            throw new BusinessRuleException(
                    "Table " + table.getTableNumber() + " has active reservations and cannot be deleted.");
        }

        tableRepository.delete(table);
    }

    @Transactional(readOnly = true)
    public RestaurantTable getEntity(Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table", id));
    }

    private boolean hasActiveReservations(Long tableId) {
        return reservationRepository
                .existsByRestaurantTableIdAndStatusIn(tableId, ReservationStatus.activeStatuses());
    }
}
