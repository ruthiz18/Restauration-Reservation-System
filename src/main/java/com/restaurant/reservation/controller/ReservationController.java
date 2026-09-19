package com.restaurant.reservation.controller;

import com.restaurant.reservation.dto.ApiResponse;
import com.restaurant.reservation.dto.ReservationRequest;
import com.restaurant.reservation.dto.ReservationResponse;
import com.restaurant.reservation.dto.ReservationStatusRequest;
import com.restaurant.reservation.entity.ReservationStatus;
import com.restaurant.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * CRUD endpoints for Entity 3 - Reservation.
 *
 * POST   /api/v1/reservations                 create
 * GET    /api/v1/reservations                 list all (?customerId= ?tableId= ?date= ?status=)
 * GET    /api/v1/reservations/{id}            read one
 * PUT    /api/v1/reservations/{id}            update
 * PATCH  /api/v1/reservations/{id}/status     move through the reservation lifecycle
 * DELETE /api/v1/reservations/{id}            delete
 */
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> create(
            @Valid @RequestBody ReservationRequest request) {

        ReservationResponse created = reservationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Reservation created successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> findAll(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long tableId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) ReservationStatus status) {

        List<ReservationResponse> reservations =
                reservationService.find(customerId, tableId, date, status);
        return ResponseEntity.ok(ApiResponse.ok(reservations.size() + " reservation(s) found", reservations));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Reservation retrieved", reservationService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ReservationRequest request) {

        return ResponseEntity.ok(ApiResponse.ok("Reservation updated successfully",
                reservationService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ReservationResponse>> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ReservationStatusRequest request) {

        ReservationResponse updated = reservationService.changeStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.ok("Reservation is now " + updated.getStatus(), updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Reservation deleted successfully"));
    }
}
