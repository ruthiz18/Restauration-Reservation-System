package com.reservo.repo;

import com.reservo.domain.Reservation;
import com.reservo.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    /** Ids of tables already held by a blocking reservation that overlaps [start, end). */
    @Query("""
            select r.table.id from Reservation r
            where r.restaurant.id = :restaurantId and r.date = :date
              and r.status in :blocking
              and r.startTime < :end and r.endTime > :start
            """)
    List<Long> findBusyTableIds(@Param("restaurantId") Long restaurantId, @Param("date") LocalDate date,
                                @Param("start") LocalTime start, @Param("end") LocalTime end,
                                @Param("blocking") Collection<ReservationStatus> blocking);

    /** One query for a whole day, so availability for all slots is computed in memory. */
    List<Reservation> findByRestaurantIdAndDateAndStatusIn(Long restaurantId, LocalDate date,
                                                           Collection<ReservationStatus> statuses);

    @Override
    @EntityGraph(attributePaths = {"restaurant", "table", "customer"})
    Page<Reservation> findAll(Specification<Reservation> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"restaurant", "table", "customer"})
    Optional<Reservation> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"restaurant", "table", "customer"})
    List<Reservation> findByStatusAndReminderSentFalseAndDate(ReservationStatus status, LocalDate date);

    boolean existsByCustomerIdAndRestaurantIdAndStatus(Long customerId, Long restaurantId, ReservationStatus status);

    @Query("select r.status, count(r) from Reservation r where r.date = :date group by r.status")
    List<Object[]> countByStatusOnDate(@Param("date") LocalDate date);
}
