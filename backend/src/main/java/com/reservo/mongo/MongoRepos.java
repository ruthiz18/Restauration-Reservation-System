package com.reservo.mongo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public final class MongoRepos {
    private MongoRepos() {}

    public interface MenuRepository extends MongoRepository<Menu, String> {
        Optional<Menu> findByRestaurantId(Long restaurantId);
    }

    public interface ReviewRepository extends MongoRepository<Review, String> {
        Page<Review> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId, Pageable pageable);
        boolean existsByRestaurantIdAndUserId(Long restaurantId, Long userId);
    }

    public interface NotificationLogRepository extends MongoRepository<NotificationLog, String> {
        Page<NotificationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    }
}
