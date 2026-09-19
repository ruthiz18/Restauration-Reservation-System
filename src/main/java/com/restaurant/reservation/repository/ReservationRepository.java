package com.restaurant.reservation.repository;

import com.restaurant.reservation.entity.Reservation;
import com.restaurant.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByCustomerIdOrderByReservationDateDescReservationTimeDesc(Long customerId);

    List<Reservation> findByRestaurantTableIdOrderByReservationDateDescReservationTimeDesc(Long tableId);

    List<Reservation> findByReservationDateOrderByReservationTimeAsc(LocalDate date);

    List<Reservation> findByStatusOrderByReservationDateAscReservationTimeAsc(ReservationStatus status);

    /** Active reservations already booked on a table for a given day - used for double-booking checks. */
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.restaurantTable.id = :tableId
              AND r.reservationDate = :date
              AND r.status IN :statuses
            """)
    List<Reservation> findActiveOnTableAndDate(@Param("tableId") Long tableId,
                                               @Param("date") LocalDate date,
                                               @Param("statuses") Collection<ReservationStatus> statuses);

    /** Used before deleting a customer (BR-05 guard). */
    boolean existsByCustomerIdAndStatusIn(Long customerId, Collection<ReservationStatus> statuses);

    /** Used before deleting a table. */
    boolean existsByRestaurantTableIdAndStatusIn(Long tableId, Collection<ReservationStatus> statuses);
}
