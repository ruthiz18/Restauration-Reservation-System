package com.restaurant.reservation.service;

import com.restaurant.reservation.dto.ReservationRequest;
import com.restaurant.reservation.dto.ReservationResponse;
import com.restaurant.reservation.entity.Customer;
import com.restaurant.reservation.entity.Reservation;
import com.restaurant.reservation.entity.ReservationStatus;
import com.restaurant.reservation.entity.RestaurantTable;
import com.restaurant.reservation.entity.TableStatus;
import com.restaurant.reservation.exception.BusinessRuleException;
import com.restaurant.reservation.exception.ResourceNotFoundException;
import com.restaurant.reservation.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Business logic for Reservation - the entity that carries most of the rules.
 *
 * Rules enforced here:
 *  R-1  the customer and the table must both exist
 *  R-2  a reservation may not be made for a date/time already in the past
 *  R-3  the requested time must fall inside opening hours
 *  R-4  party size may not exceed the capacity of the chosen table
 *  R-5  a table that is OUT_OF_SERVICE cannot be booked
 *  R-6  no double booking: two active reservations on the same table may not
 *       overlap within the configured dining slot
 *  R-7  the same customer may not hold two overlapping reservations
 *  R-8  status changes must follow the allowed lifecycle transitions
 *  R-9  a reservation in a final state (COMPLETED / CANCELLED / NO_SHOW)
 *       can no longer be edited
 */
@Service
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CustomerService customerService;
    private final RestaurantTableService tableService;

    @Value("${restaurant.reservation.slot-duration-minutes:120}")
    private int slotDurationMinutes;

    @Value("${restaurant.reservation.opening-time:09:00}")
    private String openingTime;

    @Value("${restaurant.reservation.closing-time:23:00}")
    private String closingTime;

    public ReservationService(ReservationRepository reservationRepository,
                              CustomerService customerService,
                              RestaurantTableService tableService) {
        this.reservationRepository = reservationRepository;
        this.customerService = customerService;
        this.tableService = tableService;
    }

    /** CREATE. */
    public ReservationResponse create(ReservationRequest request) {
        Customer customer = customerService.getEntity(request.getCustomerId());       // R-1
        RestaurantTable table = tableService.getEntity(request.getTableId());         // R-1

        validateSlot(request, table, customer, null);

        Reservation reservation = new Reservation();
        reservation.setCustomer(customer);
        reservation.setRestaurantTable(table);
        reservation.setReservationDate(request.getReservationDate());
        reservation.setReservationTime(request.getReservationTime());
        reservation.setPartySize(request.getPartySize());
        reservation.setNotes(request.getNotes());
        reservation.setStatus(ReservationStatus.PENDING);

        Reservation saved = reservationRepository.save(reservation);
        refreshTableStatus(table);

        return ReservationResponse.from(saved);
    }

    /** READ all. */
    @Transactional(readOnly = true)
    public List<ReservationResponse> findAll() {
        return reservationRepository.findAll().stream()
                .map(ReservationResponse::from)
                .toList();
    }

    /** READ one. */
    @Transactional(readOnly = true)
    public ReservationResponse findById(Long id) {
        return ReservationResponse.from(getEntity(id));
    }

    /** READ filtered by customer, table, date or status. */
    @Transactional(readOnly = true)
    public List<ReservationResponse> find(Long customerId, Long tableId,
                                          LocalDate date, ReservationStatus status) {
        List<Reservation> result;

        if (customerId != null) {
            result = reservationRepository
                    .findByCustomerIdOrderByReservationDateDescReservationTimeDesc(customerId);
        } else if (tableId != null) {
            result = reservationRepository
                    .findByRestaurantTableIdOrderByReservationDateDescReservationTimeDesc(tableId);
        } else if (date != null) {
            result = reservationRepository.findByReservationDateOrderByReservationTimeAsc(date);
        } else if (status != null) {
            result = reservationRepository.findByStatusOrderByReservationDateAscReservationTimeAsc(status);
        } else {
            result = reservationRepository.findAll();
        }

        return result.stream()
                .filter(r -> customerId == null || r.getCustomer().getId().equals(customerId))
                .filter(r -> tableId == null || r.getRestaurantTable().getId().equals(tableId))
                .filter(r -> date == null || r.getReservationDate().equals(date))
                .filter(r -> status == null || r.getStatus() == status)
                .map(ReservationResponse::from)
                .toList();
    }

    /** UPDATE - re-runs every rule that applied at creation. */
    public ReservationResponse update(Long id, ReservationRequest request) {
        Reservation reservation = getEntity(id);

        // R-9
        if (!reservation.getStatus().isActive()) {
            throw new BusinessRuleException(
                    "Reservation " + id + " is " + reservation.getStatus()
                            + " and can no longer be modified.");
        }

        Customer customer = customerService.getEntity(request.getCustomerId());
        RestaurantTable table = tableService.getEntity(request.getTableId());
        RestaurantTable previousTable = reservation.getRestaurantTable();

        validateSlot(request, table, customer, id);

        reservation.setCustomer(customer);
        reservation.setRestaurantTable(table);
        reservation.setReservationDate(request.getReservationDate());
        reservation.setReservationTime(request.getReservationTime());
        reservation.setPartySize(request.getPartySize());
        reservation.setNotes(request.getNotes());

        Reservation saved = reservationRepository.save(reservation);
        refreshTableStatus(table);
        if (!previousTable.getId().equals(table.getId())) {
            refreshTableStatus(previousTable);
        }

        return ReservationResponse.from(saved);
    }

    /** UPDATE status only - R-8. */
    public ReservationResponse changeStatus(Long id, ReservationStatus target) {
        Reservation reservation = getEntity(id);
        ReservationStatus current = reservation.getStatus();

        if (current == target) {
            throw new BusinessRuleException("Reservation " + id + " is already " + target);
        }
        if (!current.canTransitionTo(target)) {
            throw new BusinessRuleException(
                    "A reservation cannot move from " + current + " to " + target + ".");
        }

        reservation.setStatus(target);
        Reservation saved = reservationRepository.save(reservation);
        refreshTableStatus(reservation.getRestaurantTable());

        return ReservationResponse.from(saved);
    }

    /** DELETE - an active reservation must be cancelled before it is removed. */
    public void delete(Long id) {
        Reservation reservation = getEntity(id);

        if (reservation.getStatus() == ReservationStatus.SEATED) {
            throw new BusinessRuleException(
                    "Reservation " + id + " is currently seated and cannot be deleted. "
                            + "Complete it first.");
        }

        RestaurantTable table = reservation.getRestaurantTable();
        reservationRepository.delete(reservation);
        reservationRepository.flush();
        refreshTableStatus(table);
    }

    @Transactional(readOnly = true)
    public Reservation getEntity(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
    }

    // ------------------------------------------------------------------
    // Rule checks
    // ------------------------------------------------------------------

    /**
     * Runs rules R-2 to R-7 for a create or an update.
     *
     * @param excludeReservationId id to ignore during the overlap check, so a
     *                             reservation being edited does not clash with itself
     */
    private void validateSlot(ReservationRequest request, RestaurantTable table,
                              Customer customer, Long excludeReservationId) {

        LocalDateTime start = LocalDateTime.of(request.getReservationDate(), request.getReservationTime());
        LocalDateTime end = start.plusMinutes(slotDurationMinutes);

        // R-2
        if (start.isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("A reservation cannot be made for a date or time in the past.");
        }

        // R-3
        LocalTime opens = LocalTime.parse(openingTime);
        LocalTime closes = LocalTime.parse(closingTime);
        if (request.getReservationTime().isBefore(opens) || request.getReservationTime().isAfter(closes)) {
            throw new BusinessRuleException(
                    "Reservations are only accepted between " + opens + " and " + closes + ".");
        }

        // R-4
        if (request.getPartySize() > table.getCapacity()) {
            throw new BusinessRuleException(
                    "Party of " + request.getPartySize() + " exceeds the capacity of table "
                            + table.getTableNumber() + " (" + table.getCapacity() + " seats). "
                            + "Choose a larger table.");
        }

        // R-5
        if (table.getStatus() == TableStatus.OUT_OF_SERVICE) {
            throw new BusinessRuleException(
                    "Table " + table.getTableNumber() + " is out of service and cannot be booked.");
        }

        // R-6: no other active reservation on this table may overlap the slot
        boolean tableClash = reservationRepository
                .findActiveOnTableAndDate(table.getId(), request.getReservationDate(),
                        ReservationStatus.activeStatuses())
                .stream()
                .filter(r -> excludeReservationId == null || !r.getId().equals(excludeReservationId))
                .anyMatch(r -> overlaps(start, end, r));

        if (tableClash) {
            throw new BusinessRuleException(
                    "Table " + table.getTableNumber() + " is already booked around "
                            + request.getReservationTime() + " on " + request.getReservationDate()
                            + ". Each booking holds the table for " + slotDurationMinutes + " minutes.");
        }

        // R-7: the same customer may not be double booked either
        boolean customerClash = reservationRepository
                .findByCustomerIdOrderByReservationDateDescReservationTimeDesc(customer.getId())
                .stream()
                .filter(r -> r.getStatus().isActive())
                .filter(r -> r.getReservationDate().equals(request.getReservationDate()))
                .filter(r -> excludeReservationId == null || !r.getId().equals(excludeReservationId))
                .anyMatch(r -> overlaps(start, end, r));

        if (customerClash) {
            throw new BusinessRuleException(
                    customer.getFullName() + " already holds a reservation that overlaps this time slot.");
        }
    }

    /** Two slots overlap when each starts before the other ends. */
    private boolean overlaps(LocalDateTime start, LocalDateTime end, Reservation existing) {
        LocalDateTime existingStart = existing.getStartsAt();
        LocalDateTime existingEnd = existingStart.plusMinutes(slotDurationMinutes);
        return start.isBefore(existingEnd) && existingStart.isBefore(end);
    }

    /**
     * Keeps the table status in step with its bookings:
     * SEATED today -> OCCUPIED, an upcoming active booking -> RESERVED, otherwise AVAILABLE.
     * A table that staff put OUT_OF_SERVICE is left alone.
     */
    private void refreshTableStatus(RestaurantTable table) {
        if (table.getStatus() == TableStatus.OUT_OF_SERVICE) {
            return;
        }

        List<Reservation> reservations = reservationRepository
                .findByRestaurantTableIdOrderByReservationDateDescReservationTimeDesc(table.getId());

        boolean seatedNow = reservations.stream()
                .anyMatch(r -> r.getStatus() == ReservationStatus.SEATED);

        boolean upcoming = reservations.stream()
                .filter(r -> r.getStatus().isActive())
                .anyMatch(r -> !r.getStartsAt().plusMinutes(slotDurationMinutes)
                        .isBefore(LocalDateTime.now()));

        if (seatedNow) {
            table.setStatus(TableStatus.OCCUPIED);
        } else if (upcoming) {
            table.setStatus(TableStatus.RESERVED);
        } else {
            table.setStatus(TableStatus.AVAILABLE);
        }
    }
}
