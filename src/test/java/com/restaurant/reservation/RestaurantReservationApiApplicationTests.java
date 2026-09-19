package com.restaurant.reservation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Smoke test: the whole context, including all three CRUD stacks, starts against H2. */
@SpringBootTest
@ActiveProfiles("h2")
class RestaurantReservationApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
