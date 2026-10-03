package com.reservo.web;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Picks the background scene for a cuisine in the Thymeleaf pages (mirrors frontend/src/themes.js).
 * Scenes are static/img/&lt;key&gt;.svg; dropping a static/img/&lt;key&gt;.jpg photo overrides the illustration.
 */
@Component("themes")
public class Themes {
    private static final Map<String, List<String>> KEYWORDS = Map.of(
            "grill", List.of("grill", "bbq", "barbecue", "steak", "meat", "african", "braai", "brazil", "burger", "kebab", "nyama"),
            "sushi", List.of("japan", "sushi", "ramen", "asia", "korea", "izakaya"),
            "italian", List.of("ital", "pizza", "pasta", "trattoria", "mediterr"));

    public String key(String cuisine) {
        if (cuisine == null) return "dining";
        String c = cuisine.toLowerCase(Locale.ROOT);
        return KEYWORDS.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(c::contains))
                .map(Map.Entry::getKey)
                .findFirst().orElse("dining");
    }
}
