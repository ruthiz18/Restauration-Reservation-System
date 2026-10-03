package com.reservo.mongo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/** Flexible, nested menu per restaurant - a natural fit for a document store. */
@Document("menus")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Menu {
    @Id
    private String id;

    @Indexed(unique = true)
    private Long restaurantId;

    private List<Section> sections = new ArrayList<>();

    public record Section(@NotBlank String name, @Valid List<Item> items) {
        public Section {
            items = items == null ? List.of() : items;
        }
    }

    public record Item(@NotBlank String name, String description, @PositiveOrZero double price,
                       boolean vegetarian, List<String> tags) {
        public Item {
            tags = tags == null ? List.of() : tags;
        }
    }
}
