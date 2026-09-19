package com.restaurant.reservation.controller;

import com.restaurant.reservation.dto.ApiResponse;
import com.restaurant.reservation.dto.CustomerRequest;
import com.restaurant.reservation.dto.CustomerResponse;
import com.restaurant.reservation.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD endpoints for Entity 1 - Customer.
 *
 * POST   /api/v1/customers            create
 * GET    /api/v1/customers            list all (optional ?keyword= search)
 * GET    /api/v1/customers/{id}       read one
 * PUT    /api/v1/customers/{id}       update
 * DELETE /api/v1/customers/{id}       delete
 */
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse created = customerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Customer registered successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> findAll(
            @RequestParam(required = false) String keyword) {

        List<CustomerResponse> customers = customerService.search(keyword);
        String message = (keyword == null || keyword.isBlank())
                ? customers.size() + " customer(s) found"
                : customers.size() + " customer(s) matched \"" + keyword + "\"";
        return ResponseEntity.ok(ApiResponse.ok(message, customers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Customer retrieved", customerService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Customer updated successfully",
                customerService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Customer deleted successfully"));
    }
}
