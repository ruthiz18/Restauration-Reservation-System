package com.reservo;

import com.reservo.domain.DiningTable;
import com.reservo.domain.Restaurant;
import com.reservo.event.NotificationListener;
import com.reservo.event.ReservationEvent;
import com.reservo.mongo.MongoRepos.MenuRepository;
import com.reservo.mongo.MongoRepos.NotificationLogRepository;
import com.reservo.mongo.MongoRepos.ReviewRepository;
import com.reservo.repo.DiningTableRepository;
import com.reservo.repo.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Thymeleaf-rendered public pages and the HTML email template. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebPagesTest {

    @Autowired MockMvc mvc;
    @Autowired RestaurantRepository restaurants;
    @Autowired DiningTableRepository tables;
    @Autowired NotificationListener listener;

    @MockBean RabbitTemplate rabbit;
    @MockBean MenuRepository menus;
    @MockBean ReviewRepository reviews;
    @MockBean NotificationLogRepository logs;
    @MockBean MongoTemplate mongoTemplate;
    @MockBean GridFsTemplate gridFsTemplate; // auto-configured GridFS needs a real converter otherwise

    private Restaurant saveRestaurant(String name) {
        Restaurant r = restaurants.save(Restaurant.builder().name(name).cuisine("Thai").address("2 Test Rd")
                .description("Tasty").openTime(LocalTime.of(11, 0)).closeTime(LocalTime.of(22, 0)).build());
        tables.save(DiningTable.builder().restaurant(r).label("T1").capacity(4).build());
        return r;
    }

    @Test
    void directoryPageIsPublicAndListsRestaurants() throws Exception {
        saveRestaurant("Page Test Thai House");
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Page Test Thai House")));
        mvc.perform(get("/").param("q", "no-such-place-xyz"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No restaurants match")));
    }

    @Test
    void detailPageRendersEvenWhenMongoIsUnavailable() throws Exception {
        Restaurant r = saveRestaurant("Detail Test Grill");
        mvc.perform(get("/restaurants/" + r.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Detail Test Grill")));
    }

    @Test
    void unknownRestaurantGives404() throws Exception {
        mvc.perform(get("/restaurants/999999")).andExpect(status().isNotFound());
    }

    @Test
    void emailTemplateRendersEventDetails() {
        var event = new ReservationEvent("confirmed", 42L, "La Trattoria", "Carol", "c@t.com", null,
                LocalDate.of(2030, 5, 17), LocalTime.of(19, 30), 4, "CONFIRMED");
        String html = listener.renderHtml(event);
        assertTrue(html.contains("Your reservation is confirmed"));
        assertTrue(html.contains("La Trattoria"));
        assertTrue(html.contains("19:30"));
        assertTrue(html.contains("#42"));
    }
}
