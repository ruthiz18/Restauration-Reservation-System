package com.reservo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservo.domain.*;
import com.reservo.mongo.MongoRepos.MenuRepository;
import com.reservo.mongo.MongoRepos.NotificationLogRepository;
import com.reservo.mongo.MongoRepos.ReviewRepository;
import com.reservo.repo.DiningTableRepository;
import com.reservo.repo.RestaurantRepository;
import com.reservo.repo.UserRepository;
import com.reservo.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired RestaurantRepository restaurants;
    @Autowired DiningTableRepository tables;
    @Autowired JwtService jwt;

    // MongoDB and RabbitMQ are not needed to exercise the relational/security logic.
    @MockBean RabbitTemplate rabbit;
    @MockBean MenuRepository menus;
    @MockBean ReviewRepository reviews;
    @MockBean NotificationLogRepository logs;
    @MockBean MongoTemplate mongoTemplate;
    @MockBean GridFsTemplate gridFsTemplate; // auto-configured GridFS needs a real converter otherwise

    Restaurant restaurant;
    String adminToken;
    String customerToken;

    @BeforeEach
    void setUp() {
        String suffix = String.valueOf(System.nanoTime());
        User admin = users.save(User.builder().fullName("Admin").email("admin" + suffix + "@t.com")
                .passwordHash("x").role(Role.ADMIN).provider(AuthProvider.LOCAL).build());
        User customer = users.save(User.builder().fullName("Cust").email("c" + suffix + "@t.com")
                .passwordHash("x").role(Role.CUSTOMER).provider(AuthProvider.LOCAL).build());
        adminToken = jwt.generate(admin);
        customerToken = jwt.generate(customer);

        restaurant = restaurants.save(Restaurant.builder().name("Test Bistro " + suffix).cuisine("French")
                .address("1 Main St").openTime(LocalTime.of(11, 0)).closeTime(LocalTime.of(22, 0)).build());
        tables.save(DiningTable.builder().restaurant(restaurant).label("T1").capacity(4).build());
    }

    private String body(LocalDate date, int party) throws Exception {
        return json.writeValueAsString(java.util.Map.of("restaurantId", restaurant.getId(),
                "date", date.toString(), "time", "19:00", "partySize", party));
    }

    private ResultActions book(String token, int party) throws Exception {
        return mvc.perform(post("/api/reservations").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body(LocalDate.now().plusDays(1), party)));
    }

    @Test
    void registerThenLoginReturnsToken() throws Exception {
        String email = "new" + System.nanoTime() + "@t.com";
        String reg = json.writeValueAsString(java.util.Map.of("fullName", "New User", "email", email,
                "password", "Password123"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(reg))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("email", email, "password", "Password123"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("email", email, "password", "wrong-pass"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/reservations/mine")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/restaurants")).andExpect(status().isOk()); // public
    }

    @Test
    void rbacBlocksCustomersFromAdminAndStaffEndpoints() throws Exception {
        String req = json.writeValueAsString(java.util.Map.of("name", "X", "cuisine", "Y", "address", "Z",
                "openTime", "10:00", "closeTime", "20:00"));
        mvc.perform(post("/api/restaurants").header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON).content(req)).andExpect(status().isForbidden());
        mvc.perform(post("/api/restaurants").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(req)).andExpect(status().isCreated());
        mvc.perform(get("/api/reservations").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
    }

    @Test
    void bookingReservesTableAndPublishesEvent() throws Exception {
        book(customerToken, 2).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.tableLabel").value("T1"));
        verify(rabbit, atLeastOnce()).convertAndSend(eq("reservo.events"), eq("reservation.created"), any(Object.class));
    }

    @Test
    void doubleBookingTheOnlyTableIsRejected() throws Exception {
        book(customerToken, 2).andExpect(status().isCreated());
        book(customerToken, 2).andExpect(status().isConflict());
    }

    @Test
    void partyLargerThanAnyTableIsRejected() throws Exception {
        book(customerToken, 12).andExpect(status().isBadRequest());
    }

    @Test
    void pastDateIsRejected() throws Exception {
        mvc.perform(post("/api/reservations").header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body(LocalDate.now().minusDays(1), 2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void availabilityDropsAfterBooking() throws Exception {
        LocalDate day = LocalDate.now().plusDays(1);
        String url = "/api/restaurants/" + restaurant.getId() + "/availability?date=" + day + "&partySize=2";
        JsonNode before = json.readTree(mvc.perform(get(url)).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString());
        book(customerToken, 2).andExpect(status().isCreated());
        JsonNode after = json.readTree(mvc.perform(get(url)).andReturn().getResponse().getContentAsString());
        int slot19Before = free(before, "19:00");
        int slot19After = free(after, "19:00");
        assertEquals(1, slot19Before);
        assertEquals(0, slot19After);
        assertEquals(1, free(after, "11:00")); // unaffected slot far from 19:00
    }

    @Test
    void staffLifecycleAndCustomerCancellationRules() throws Exception {
        JsonNode created = json.readTree(book(customerToken, 2).andReturn().getResponse().getContentAsString());
        long id = created.get("id").asLong();
        String patch = "/api/reservations/" + id + "/status";
        // customer cannot confirm
        mvc.perform(patch(patch).header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isForbidden());
        // admin confirms, cannot jump to COMPLETED
        mvc.perform(patch(patch).header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(patch(patch).header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isConflict());
        // customer cancels own reservation, freeing the table
        mvc.perform(patch(patch).header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());
        book(customerToken, 2).andExpect(status().isCreated());
    }

    @Test
    void customersCannotReadOtherCustomersReservations() throws Exception {
        long id = json.readTree(book(customerToken, 2).andReturn().getResponse().getContentAsString()).get("id").asLong();
        User other = users.save(User.builder().fullName("Other").email("o" + System.nanoTime() + "@t.com")
                .passwordHash("x").role(Role.CUSTOMER).provider(AuthProvider.LOCAL).build());
        mvc.perform(get("/api/reservations/" + id).header("Authorization", "Bearer " + jwt.generate(other)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/reservations/" + id).header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }

    private static int free(JsonNode slots, String time) {
        for (JsonNode s : slots) {
            if (s.get("time").asText().startsWith(time)) return s.get("availableTables").asInt();
        }
        throw new AssertionError("slot not found: " + time + " in " + slots);
    }
}
