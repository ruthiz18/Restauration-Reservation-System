package com.reservo.web;

import com.reservo.dto.Dtos.PageDto;
import com.reservo.dto.Dtos.RatingSummary;
import com.reservo.dto.Dtos.RestaurantDto;
import com.reservo.exception.ApiException;
import com.reservo.mongo.Menu;
import com.reservo.service.RestaurantService;
import com.reservo.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Server-rendered (Thymeleaf) public pages: a crawlable restaurant directory and detail page.
 * Booking itself happens in the React SPA, which these pages link to.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PageController {
    private final RestaurantService restaurants;
    private final ReviewService reviews;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping("/")
    public String home(@RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        PageDto<RestaurantDto> result = restaurants.search(q, Math.max(page, 0), 9);
        model.addAttribute("restaurants", result.content());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("page", result.page());
        model.addAttribute("hasPrev", result.page() > 0);
        model.addAttribute("hasNext", (long) (result.page() + 1) * result.size() < result.total());
        model.addAttribute("frontendUrl", frontendUrl);
        return "index";
    }

    @GetMapping("/restaurants/{id}")
    public String restaurant(@PathVariable Long id, Model model) {
        RestaurantDto restaurant;
        try {
            restaurant = restaurants.get(id);
        } catch (ApiException e) {
            throw new ResponseStatusException(e.getStatus(), e.getMessage());
        }
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("menu", safeMenu(id));
        model.addAttribute("rating", safeRating(id));
        model.addAttribute("frontendUrl", frontendUrl);
        return "restaurant";
    }

    // MongoDB-backed content must never take the whole page down if the document store is unavailable.
    private Menu safeMenu(Long id) {
        try {
            return restaurants.getMenu(id);
        } catch (Exception e) {
            log.warn("Menu unavailable for restaurant {}: {}", id, e.getMessage());
            return null;
        }
    }

    private RatingSummary safeRating(Long id) {
        try {
            RatingSummary r = reviews.summary(id);
            return r == null ? new RatingSummary(0, 0) : r;
        } catch (Exception e) {
            log.warn("Rating unavailable for restaurant {}: {}", id, e.getMessage());
            return new RatingSummary(0, 0);
        }
    }
}
