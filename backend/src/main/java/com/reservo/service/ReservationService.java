package com.reservo.service;

import com.reservo.domain.*;
import com.reservo.dto.Dtos.*;
import com.reservo.event.ReservationEvent;
import com.reservo.exception.ApiException;
import com.reservo.repo.DiningTableRepository;
import com.reservo.repo.ReservationRepository;
import com.reservo.repo.RestaurantRepository;
import com.reservo.repo.UserRepository;
import com.reservo.security.AuthUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class ReservationService {
    private final ReservationRepository reservations;
    private final RestaurantRepository restaurants;
    private final DiningTableRepository tables;
    private final UserRepository users;
    private final ApplicationEventPublisher events;
    private final int durationMinutes;
    private final int stepMinutes;

    public ReservationService(ReservationRepository reservations, RestaurantRepository restaurants,
                              DiningTableRepository tables, UserRepository users, ApplicationEventPublisher events,
                              @Value("${app.reservation.duration-minutes}") int durationMinutes,
                              @Value("${app.reservation.slot-step-minutes}") int stepMinutes) {
        this.reservations = reservations;
        this.restaurants = restaurants;
        this.tables = tables;
        this.users = users;
        this.events = events;
        this.durationMinutes = durationMinutes;
        this.stepMinutes = stepMinutes;
    }

    /** Bookable start times for a party size, with how many tables are still free in each. */
    @Transactional(readOnly = true)
    public List<SlotDto> availability(Long restaurantId, LocalDate date, int partySize) {
        Restaurant restaurant = activeRestaurant(restaurantId);
        List<DiningTable> fitting = tables.findByRestaurantIdAndActiveTrueOrderByCapacityAscLabelAsc(restaurantId)
                .stream().filter(t -> t.getCapacity() >= partySize).toList();
        List<Reservation> existing = reservations.findByRestaurantIdAndDateAndStatusIn(
                restaurantId, date, ReservationStatus.BLOCKING);

        List<SlotDto> slots = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (LocalTime t = restaurant.getOpenTime();
             !t.plusMinutes(durationMinutes).isAfter(restaurant.getCloseTime());
             t = t.plusMinutes(stepMinutes)) {
            if (LocalDateTime.of(date, t).isBefore(now)) continue;
            final LocalTime start = t;
            final LocalTime end = t.plusMinutes(durationMinutes);
            Set<Long> busy = new HashSet<>();
            for (Reservation r : existing) {
                if (r.getStartTime().isBefore(end) && r.getEndTime().isAfter(start)) busy.add(r.getTable().getId());
            }
            long free = fitting.stream().filter(tb -> !busy.contains(tb.getId())).count();
            slots.add(new SlotDto(start, (int) free));
            if (t.plusMinutes(stepMinutes).isBefore(t)) break; // wrapped past midnight
        }
        return slots;
    }

    @Transactional
    public ReservationDto create(AuthUser actor, ReservationRequest req) {
        // Row lock on the restaurant serialises concurrent bookings, preventing double-booking a table.
        Restaurant restaurant = restaurants.findByIdForUpdate(req.restaurantId())
                .filter(Restaurant::isActive).orElseThrow(() -> ApiException.notFound("Restaurant"));

        LocalTime end = req.time().plusMinutes(durationMinutes);
        if (req.time().isBefore(restaurant.getOpenTime()) || end.isAfter(restaurant.getCloseTime())
                || end.isBefore(req.time())) {
            throw ApiException.badRequest("Selected time is outside opening hours (%s - %s)"
                    .formatted(restaurant.getOpenTime(), restaurant.getCloseTime()));
        }
        if (LocalDateTime.of(req.date(), req.time()).isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("Reservation time must be in the future");
        }

        List<DiningTable> fitting = tables.findByRestaurantIdAndActiveTrueOrderByCapacityAscLabelAsc(restaurant.getId())
                .stream().filter(t -> t.getCapacity() >= req.partySize()).toList();
        if (fitting.isEmpty()) {
            throw ApiException.badRequest("No table can seat a party of " + req.partySize());
        }
        Set<Long> busy = new HashSet<>(reservations.findBusyTableIds(
                restaurant.getId(), req.date(), req.time(), end, ReservationStatus.BLOCKING));
        DiningTable table = fitting.stream().filter(t -> !busy.contains(t.getId())).findFirst()
                .orElseThrow(() -> ApiException.conflict("No table available at that time - please pick another slot"));

        User customer = users.getReferenceById(actor.id());
        Reservation saved = reservations.save(Reservation.builder()
                .customer(customer).restaurant(restaurant).table(table)
                .date(req.date()).startTime(req.time()).endTime(end).partySize(req.partySize())
                .specialRequests(req.specialRequests()).status(ReservationStatus.PENDING).build());

        // Reload recipient details for the event (getReferenceById returned an uninitialised proxy).
        User full = users.findById(actor.id()).orElseThrow();
        saved.setCustomer(full);
        events.publishEvent(event("created", saved));
        return ReservationDto.from(saved);
    }

    @Transactional(readOnly = true)
    public PageDto<ReservationDto> mine(AuthUser actor, int page, int size) {
        Specification<Reservation> spec = (root, q, cb) -> cb.equal(root.get("customer").get("id"), actor.id());
        return PageDto.of(reservations.findAll(spec, PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Order.desc("date"), Sort.Order.desc("startTime")))).map(ReservationDto::from));
    }

    @Transactional(readOnly = true)
    public PageDto<ReservationDto> search(Long restaurantId, LocalDate date, ReservationStatus status,
                                          int page, int size) {
        Specification<Reservation> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (restaurantId != null) ps.add(cb.equal(root.get("restaurant").get("id"), restaurantId));
            if (date != null) ps.add(cb.equal(root.get("date"), date));
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return PageDto.of(reservations.findAll(spec, PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Order.desc("date"), Sort.Order.asc("startTime")))).map(ReservationDto::from));
    }

    @Transactional(readOnly = true)
    public ReservationDto get(AuthUser actor, Long id) {
        Reservation r = load(id);
        assertCanAccess(actor, r);
        return ReservationDto.from(r);
    }

    @Transactional
    public ReservationDto changeStatus(AuthUser actor, Long id, ReservationStatus target) {
        Reservation r = load(id);
        assertCanAccess(actor, r);
        if (!actor.isStaffOrAdmin() && target != ReservationStatus.CANCELLED) {
            throw ApiException.forbidden("Customers may only cancel their own reservations");
        }
        if (!r.getStatus().canMoveTo(target)) {
            throw ApiException.conflict("Cannot change a %s reservation to %s".formatted(r.getStatus(), target));
        }
        r.setStatus(target);
        String type = switch (target) {
            case CONFIRMED -> "confirmed";
            case CANCELLED -> "cancelled";
            default -> "status";
        };
        events.publishEvent(event(type, r));
        return ReservationDto.from(r);
    }

    @Transactional(readOnly = true)
    public StatsDto stats() {
        LocalDate today = LocalDate.now();
        Map<ReservationStatus, Long> byStatus = new EnumMap<>(ReservationStatus.class);
        for (ReservationStatus s : ReservationStatus.values()) byStatus.put(s, 0L);
        reservations.countByStatusOnDate(today).forEach(row -> byStatus.put((ReservationStatus) row[0], (Long) row[1]));
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new StatsDto(today, total, byStatus, restaurants.count(), users.count());
    }

    public static ReservationEvent event(String type, Reservation r) {
        return new ReservationEvent(type, r.getId(), r.getRestaurant().getName(), r.getCustomer().getFullName(),
                r.getCustomer().getEmail(), r.getCustomer().getPhone(), r.getDate(), r.getStartTime(),
                r.getPartySize(), r.getStatus().name());
    }

    private Reservation load(Long id) {
        return reservations.findWithDetailsById(id).orElseThrow(() -> ApiException.notFound("Reservation"));
    }

    private void assertCanAccess(AuthUser actor, Reservation r) {
        if (!actor.isStaffOrAdmin() && !r.getCustomer().getId().equals(actor.id())) {
            throw ApiException.forbidden("You can only access your own reservations");
        }
    }

    private Restaurant activeRestaurant(Long id) {
        return restaurants.findById(id).filter(Restaurant::isActive)
                .orElseThrow(() -> ApiException.notFound("Restaurant"));
    }
}
