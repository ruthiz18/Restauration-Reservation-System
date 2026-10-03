package com.reservo.service;

import com.reservo.domain.ReservationStatus;
import com.reservo.domain.User;
import com.reservo.dto.Dtos.PageDto;
import com.reservo.dto.Dtos.RatingSummary;
import com.reservo.dto.Dtos.ReviewRequest;
import com.reservo.exception.ApiException;
import com.reservo.mongo.MongoRepos.ReviewRepository;
import com.reservo.mongo.Review;
import com.reservo.repo.ReservationRepository;
import com.reservo.repo.RestaurantRepository;
import com.reservo.repo.UserRepository;
import com.reservo.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviews;
    private final ReservationRepository reservations;
    private final RestaurantRepository restaurants;
    private final UserRepository users;
    private final MongoTemplate mongo;

    public Review create(AuthUser actor, Long restaurantId, ReviewRequest req) {
        if (!restaurants.existsById(restaurantId)) throw ApiException.notFound("Restaurant");
        // Business rule: only guests who actually dined can leave a review.
        if (!reservations.existsByCustomerIdAndRestaurantIdAndStatus(actor.id(), restaurantId, ReservationStatus.COMPLETED)) {
            throw ApiException.forbidden("You can review a restaurant after a completed reservation");
        }
        if (reviews.existsByRestaurantIdAndUserId(restaurantId, actor.id())) {
            throw ApiException.conflict("You have already reviewed this restaurant");
        }
        User user = users.findById(actor.id()).orElseThrow(() -> ApiException.notFound("User"));
        return reviews.save(Review.builder().restaurantId(restaurantId).userId(actor.id())
                .userName(user.getFullName()).rating(req.rating()).comment(req.comment()).build());
    }

    public PageDto<Review> list(Long restaurantId, int page, int size) {
        return PageDto.of(reviews.findByRestaurantIdOrderByCreatedAtDesc(restaurantId,
                PageRequest.of(page, Math.min(size, 50))));
    }

    public RatingSummary summary(Long restaurantId) {
        var agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("restaurantId").is(restaurantId)),
                Aggregation.group().avg("rating").as("average").count().as("count"));
        Document doc = mongo.aggregate(agg, "reviews", Document.class).getUniqueMappedResult();
        if (doc == null) return new RatingSummary(0, 0);
        double avg = Math.round(((Number) doc.get("average")).doubleValue() * 10) / 10.0;
        return new RatingSummary(avg, ((Number) doc.get("count")).longValue());
    }
}
