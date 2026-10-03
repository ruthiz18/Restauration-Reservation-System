package com.reservo.web;

import com.reservo.dto.Dtos.*;
import com.reservo.mongo.Menu;
import com.reservo.mongo.Review;
import com.reservo.security.AuthUser;
import com.reservo.service.RestaurantService;
import com.reservo.service.ReservationService;
import com.reservo.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RestaurantController {
    private final RestaurantService restaurants;
    private final ReservationService reservations;
    private final ReviewService reviews;

    // ---- public browsing ----
    @GetMapping("/restaurants")
    public PageDto<RestaurantDto> search(@RequestParam(required = false) String q,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "12") int size) {
        return restaurants.search(q, page, size);
    }

    @GetMapping("/restaurants/{id}")
    public RestaurantDto get(@PathVariable Long id) {
        return restaurants.get(id);
    }

    @GetMapping("/restaurants/{id}/availability")
    public List<SlotDto> availability(@PathVariable Long id,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                      @RequestParam(defaultValue = "2") int partySize) {
        return reservations.availability(id, date, partySize);
    }

    @GetMapping("/restaurants/{id}/menu")
    public Menu menu(@PathVariable Long id) {
        return restaurants.getMenu(id);
    }

    @GetMapping("/restaurants/{id}/reviews")
    public PageDto<Review> reviews(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        return reviews.list(id, page, size);
    }

    @GetMapping("/restaurants/{id}/rating")
    public RatingSummary rating(@PathVariable Long id) {
        return reviews.summary(id);
    }

    // ---- admin: restaurants & tables ----
    @PostMapping("/restaurants")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public RestaurantDto create(@Valid @RequestBody RestaurantRequest req) {
        return restaurants.create(req);
    }

    @PutMapping("/restaurants/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RestaurantDto update(@PathVariable Long id, @Valid @RequestBody RestaurantRequest req) {
        return restaurants.update(id, req);
    }

    @DeleteMapping("/restaurants/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivate(@PathVariable Long id) {
        restaurants.deactivate(id);
    }

    @GetMapping("/restaurants/{id}/tables")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public List<TableDto> tables(@PathVariable Long id) {
        return restaurants.listTables(id);
    }

    @PostMapping("/restaurants/{id}/tables")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TableDto addTable(@PathVariable Long id, @Valid @RequestBody TableRequest req) {
        return restaurants.addTable(id, req);
    }

    @PutMapping("/tables/{tableId}")
    @PreAuthorize("hasRole('ADMIN')")
    public TableDto updateTable(@PathVariable Long tableId, @Valid @RequestBody TableRequest req) {
        return restaurants.updateTable(tableId, req);
    }

    @DeleteMapping("/tables/{tableId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivateTable(@PathVariable Long tableId) {
        restaurants.deactivateTable(tableId);
    }

    // ---- staff: menu; customers: reviews ----
    @PutMapping("/restaurants/{id}/menu")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public Menu saveMenu(@PathVariable Long id, @Valid @RequestBody List<Menu.@Valid Section> sections) {
        return restaurants.saveMenu(id, sections);
    }

    @PostMapping("/restaurants/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public Review review(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                         @Valid @RequestBody ReviewRequest req) {
        return reviews.create(user, id, req);
    }
}
