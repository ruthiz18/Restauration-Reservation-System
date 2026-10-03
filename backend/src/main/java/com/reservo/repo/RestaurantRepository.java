package com.reservo.repo;

import com.reservo.domain.Restaurant;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    @Query("""
            select r from Restaurant r
            where r.active = true
              and (lower(r.name) like lower(concat('%', :q, '%'))
                   or lower(r.cuisine) like lower(concat('%', :q, '%')))
            """)
    Page<Restaurant> search(@Param("q") String q, Pageable pageable);

    /** Serialises concurrent bookings for the same restaurant so two requests cannot take the same table. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Restaurant r where r.id = :id")
    Optional<Restaurant> findByIdForUpdate(@Param("id") Long id);
}
