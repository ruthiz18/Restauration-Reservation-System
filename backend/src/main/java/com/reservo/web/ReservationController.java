package com.reservo.web;

import com.reservo.domain.ReservationStatus;
import com.reservo.dto.Dtos.*;
import com.reservo.security.AuthUser;
import com.reservo.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservations;

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ReservationDto create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ReservationRequest req) {
        return reservations.create(user, req);
    }

    @GetMapping("/reservations/mine")
    public PageDto<ReservationDto> mine(@AuthenticationPrincipal AuthUser user,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return reservations.mine(user, page, size);
    }

    @GetMapping("/reservations")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public PageDto<ReservationDto> search(@RequestParam(required = false) Long restaurantId,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                          @RequestParam(required = false) ReservationStatus status,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return reservations.search(restaurantId, date, status, page, size);
    }

    @GetMapping("/reservations/{id}")
    public ReservationDto get(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return reservations.get(user, id);
    }

    @PatchMapping("/reservations/{id}/status")
    public ReservationDto status(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                 @Valid @RequestBody StatusRequest req) {
        return reservations.changeStatus(user, id, req.status());
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public StatsDto stats() {
        return reservations.stats();
    }
}
