package com.reservo.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("reviews")
@CompoundIndex(name = "restaurant_user_idx", def = "{'restaurantId': 1, 'userId': 1}", unique = true)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {
    @Id
    private String id;
    private Long restaurantId;
    private Long userId;
    private String userName;
    private int rating;
    private String comment;
    @Builder.Default
    private Instant createdAt = Instant.now();
}
