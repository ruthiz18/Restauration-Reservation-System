package com.reservo.config;

import com.reservo.domain.*;
import com.reservo.mongo.Menu;
import com.reservo.mongo.Menu.Item;
import com.reservo.mongo.Menu.Section;
import com.reservo.mongo.MongoRepos.MenuRepository;
import com.reservo.repo.DiningTableRepository;
import com.reservo.repo.RestaurantRepository;
import com.reservo.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

/** Seeds demo accounts, restaurants, tables and menus on first start. */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {
    private final UserRepository users;
    private final RestaurantRepository restaurants;
    private final DiningTableRepository tables;
    private final MenuRepository menus;
    private final PasswordEncoder encoder;

    @Value("${app.seed.admin-email}") private String adminEmail;
    @Value("${app.seed.admin-password}") private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() == 0) {
            user("Admin", adminEmail, adminPassword, Role.ADMIN);
            user("Sam Staff", "staff@reservo.local", "Staff@123", Role.STAFF);
            user("Carol Customer", "customer@reservo.local", "Customer@123", Role.CUSTOMER);
            log.info("Seeded demo users (admin: {})", adminEmail);
        }
        if (restaurants.count() == 0) {
            seedRestaurant("La Trattoria", "Italian", "12 Kigali Heights Rd",
                    "Handmade pasta and wood-fired pizza in a cosy dining room.", LocalTime.of(11, 0), LocalTime.of(22, 0),
                    new Section("Starters", List.of(new Item("Bruschetta", "Tomato, basil, garlic", 6.5, true, List.of("vegan")),
                            new Item("Calamari", "Lightly fried, lemon aioli", 9.0, false, List.of()))),
                    new Section("Mains", List.of(new Item("Margherita Pizza", "San Marzano, mozzarella", 12.0, true, List.of()),
                            new Item("Tagliatelle al Ragù", "Slow-cooked beef ragù", 15.5, false, List.of()))));
            seedRestaurant("Sakura Sushi", "Japanese", "4 Nyarutarama Ave",
                    "Fresh sushi, ramen and izakaya small plates.", LocalTime.of(12, 0), LocalTime.of(23, 0),
                    new Section("Sushi", List.of(new Item("Salmon Nigiri", "2 pieces", 7.0, false, List.of("gluten-free")),
                            new Item("Avocado Roll", "8 pieces", 6.0, true, List.of("vegan")))),
                    new Section("Ramen", List.of(new Item("Tonkotsu Ramen", "Pork broth, chashu, egg", 14.0, false, List.of()))));
            seedRestaurant("Savanna Grill", "African", "9 Kimihurura Close",
                    "Charcoal-grilled meats, brochettes and local favourites.", LocalTime.of(10, 0), LocalTime.of(21, 30),
                    new Section("Grill", List.of(new Item("Goat Brochette", "With plantain and pili-pili", 8.5, false, List.of()),
                            new Item("Grilled Tilapia", "Whole fish, ugali", 13.0, false, List.of()))),
                    new Section("Sides", List.of(new Item("Sweet Potato Fries", null, 4.0, true, List.of()))));
            log.info("Seeded demo restaurants");
        }
    }

    private void user(String name, String email, String password, Role role) {
        users.save(User.builder().fullName(name).email(email).passwordHash(encoder.encode(password))
                .role(role).provider(AuthProvider.LOCAL).build());
    }

    private void seedRestaurant(String name, String cuisine, String address, String description,
                                LocalTime open, LocalTime close, Section... sections) {
        Restaurant r = restaurants.save(Restaurant.builder().name(name).cuisine(cuisine).address(address)
                .description(description).phone("+250 788 000 000").openTime(open).closeTime(close).build());
        int[] capacities = {2, 2, 4, 4, 4, 6, 8};
        for (int i = 0; i < capacities.length; i++) {
            tables.save(DiningTable.builder().restaurant(r).label("T" + (i + 1)).capacity(capacities[i]).build());
        }
        Menu menu = new Menu();
        menu.setRestaurantId(r.getId());
        menu.setSections(List.of(sections));
        menus.save(menu);
    }
}
