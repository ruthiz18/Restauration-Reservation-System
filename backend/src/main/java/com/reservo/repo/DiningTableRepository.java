package com.reservo.repo;

import com.reservo.domain.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {
    List<DiningTable> findByRestaurantIdOrderByCapacityAscLabelAsc(Long restaurantId);
    List<DiningTable> findByRestaurantIdAndActiveTrueOrderByCapacityAscLabelAsc(Long restaurantId);
    boolean existsByRestaurantIdAndLabelIgnoreCase(Long restaurantId, String label);
}
