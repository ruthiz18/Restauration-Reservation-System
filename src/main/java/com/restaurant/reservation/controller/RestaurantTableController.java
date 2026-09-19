package com.restaurant.reservation.controller;

import com.restaurant.reservation.dto.ApiResponse;
import com.restaurant.reservation.dto.RestaurantTableRequest;
import com.restaurant.reservation.dto.RestaurantTableResponse;
import com.restaurant.reservation.entity.TableStatus;
import com.restaurant.reservation.service.RestaurantTableService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD endpoints for Entity 2 - RestaurantTable.
 *
 * POST   /api/v1/tables               create
 * GET    /api/v1/tables               list all (?status= ?minCapacity= ?keyword=)
 * GET    /api/v1/tables/{id}          read one
 * PUT    /api/v1/tables/{id}          update
 * DELETE /api/v1/tables/{id}          delete
 */
@RestController
@RequestMapping("/api/v1/tables")
public class RestaurantTableController {

    private final RestaurantTableService tableService;

    public RestaurantTableController(RestaurantTableService tableService) {
        this.tableService = tableService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RestaurantTableResponse>> create(
            @Valid @RequestBody RestaurantTableRequest request) {

        RestaurantTableResponse created = tableService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Table created successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RestaurantTableResponse>>> findAll(
            @RequestParam(required = false) TableStatus status,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) String keyword) {

        List<RestaurantTableResponse> tables = tableService.find(status, minCapacity, keyword);
        return ResponseEntity.ok(ApiResponse.ok(tables.size() + " table(s) found", tables));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RestaurantTableResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Table retrieved", tableService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RestaurantTableResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantTableRequest request) {

        return ResponseEntity.ok(ApiResponse.ok("Table updated successfully",
                tableService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        tableService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Table deleted successfully"));
    }
}
