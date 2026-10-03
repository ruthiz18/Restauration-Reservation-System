package com.reservo.dto;

import com.reservo.domain.*;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    // ---- auth / users ----
    public record RegisterRequest(@NotBlank @Size(max = 120) String fullName,
                                  @NotBlank @Email String email,
                                  @Size(max = 30) String phone,
                                  @NotBlank @Size(min = 8, max = 72, message = "must be 8-72 characters") String password) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record UserDto(Long id, String fullName, String email, String phone, Role role,
                          AuthProvider provider, boolean enabled) {
        public static UserDto from(User u) {
            return new UserDto(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole(),
                    u.getProvider(), u.isEnabled());
        }
    }

    public record AuthResponse(String token, UserDto user) {}
    public record RoleRequest(@NotNull Role role) {}
    public record EnabledRequest(boolean enabled) {}

    // ---- restaurants ----
    public record RestaurantRequest(@NotBlank @Size(max = 120) String name,
                                    @Size(max = 1000) String description,
                                    @NotBlank @Size(max = 60) String cuisine,
                                    @NotBlank @Size(max = 200) String address,
                                    @Size(max = 30) String phone,
                                    @NotNull LocalTime openTime,
                                    @NotNull LocalTime closeTime) {}

    public record RestaurantDto(Long id, String name, String description, String cuisine, String address,
                                String phone, LocalTime openTime, LocalTime closeTime, boolean active) {
        public static RestaurantDto from(Restaurant r) {
            return new RestaurantDto(r.getId(), r.getName(), r.getDescription(), r.getCuisine(), r.getAddress(),
                    r.getPhone(), r.getOpenTime(), r.getCloseTime(), r.isActive());
        }
    }

    public record TableRequest(@NotBlank @Size(max = 30) String label, @Min(1) @Max(30) int capacity) {}

    public record TableDto(Long id, Long restaurantId, String label, int capacity, boolean active) {
        public static TableDto from(DiningTable t) {
            return new TableDto(t.getId(), t.getRestaurant().getId(), t.getLabel(), t.getCapacity(), t.isActive());
        }
    }

    // ---- reservations ----
    public record ReservationRequest(@NotNull Long restaurantId,
                                     @NotNull LocalDate date,
                                     @NotNull LocalTime time,
                                     @Min(1) @Max(30) int partySize,
                                     @Size(max = 500) String specialRequests) {}

    public record StatusRequest(@NotNull ReservationStatus status) {}

    public record SlotDto(LocalTime time, int availableTables) {}

    public record ReservationDto(Long id, Long restaurantId, String restaurantName, Long tableId, String tableLabel,
                                 Long customerId, String customerName, String customerEmail, LocalDate date,
                                 LocalTime startTime, LocalTime endTime, int partySize, String specialRequests,
                                 ReservationStatus status, Instant createdAt) {
        public static ReservationDto from(Reservation r) {
            return new ReservationDto(r.getId(), r.getRestaurant().getId(), r.getRestaurant().getName(),
                    r.getTable().getId(), r.getTable().getLabel(), r.getCustomer().getId(),
                    r.getCustomer().getFullName(), r.getCustomer().getEmail(), r.getDate(), r.getStartTime(),
                    r.getEndTime(), r.getPartySize(), r.getSpecialRequests(), r.getStatus(), r.getCreatedAt());
        }
    }

    // ---- reviews ----
    public record ReviewRequest(@Min(1) @Max(5) int rating, @Size(max = 1000) String comment) {}
    public record RatingSummary(double average, long count) {}

    // ---- misc ----
    public record PageDto<T>(List<T> content, long total, int page, int size) {
        public static <T> PageDto<T> of(org.springframework.data.domain.Page<T> p) {
            return new PageDto<>(p.getContent(), p.getTotalElements(), p.getNumber(), p.getSize());
        }
    }

    public record StatsDto(LocalDate date, long totalToday, java.util.Map<ReservationStatus, Long> byStatus,
                           long restaurants, long users) {}
}
