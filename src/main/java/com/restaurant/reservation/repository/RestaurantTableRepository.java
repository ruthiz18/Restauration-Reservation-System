package com.restaurant.reservation.repository;

import com.restaurant.reservation.entity.RestaurantTable;
import com.restaurant.reservation.entity.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {

    Optional<RestaurantTable> findByTableNumberIgnoreCase(String tableNumber);

    boolean existsByTableNumberIgnoreCase(String tableNumber);

    List<RestaurantTable> findByStatus(TableStatus status);

    List<RestaurantTable> findByCapacityGreaterThanEqualOrderByCapacityAsc(Integer capacity);

    @Query("""
            SELECT t FROM RestaurantTable t
            WHERE LOWER(t.tableNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(t.location)    LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY t.tableNumber ASC
            """)
    List<RestaurantTable> search(@Param("keyword") String keyword);
}
