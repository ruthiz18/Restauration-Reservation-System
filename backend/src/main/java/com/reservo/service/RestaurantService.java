package com.reservo.service;

import com.reservo.domain.DiningTable;
import com.reservo.domain.Restaurant;
import com.reservo.dto.Dtos.*;
import com.reservo.exception.ApiException;
import com.reservo.mongo.Menu;
import com.reservo.mongo.MongoRepos.MenuRepository;
import com.reservo.repo.DiningTableRepository;
import com.reservo.repo.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService {
    private final RestaurantRepository restaurants;
    private final DiningTableRepository tables;
    private final MenuRepository menus;

    @Transactional(readOnly = true)
    public PageDto<RestaurantDto> search(String q, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("name"));
        return PageDto.of(restaurants.search(q == null ? "" : q.trim(), pageable).map(RestaurantDto::from));
    }

    @Cacheable(value = "restaurants", key = "#id")
    @Transactional(readOnly = true)
    public RestaurantDto get(Long id) {
        return RestaurantDto.from(find(id));
    }

    @Transactional
    public RestaurantDto create(RestaurantRequest req) {
        Restaurant r = new Restaurant();
        apply(r, req);
        return RestaurantDto.from(restaurants.save(r));
    }

    @CacheEvict(value = "restaurants", key = "#id")
    @Transactional
    public RestaurantDto update(Long id, RestaurantRequest req) {
        Restaurant r = find(id);
        apply(r, req);
        return RestaurantDto.from(r);
    }

    @CacheEvict(value = "restaurants", key = "#id")
    @Transactional
    public void deactivate(Long id) {
        find(id).setActive(false);
    }

    // ---- tables ----
    @Transactional(readOnly = true)
    public List<TableDto> listTables(Long restaurantId) {
        find(restaurantId);
        return tables.findByRestaurantIdOrderByCapacityAscLabelAsc(restaurantId).stream().map(TableDto::from).toList();
    }

    @Transactional
    public TableDto addTable(Long restaurantId, TableRequest req) {
        Restaurant r = find(restaurantId);
        if (tables.existsByRestaurantIdAndLabelIgnoreCase(restaurantId, req.label())) {
            throw ApiException.conflict("A table with this label already exists");
        }
        return TableDto.from(tables.save(DiningTable.builder()
                .restaurant(r).label(req.label().trim()).capacity(req.capacity()).build()));
    }

    @Transactional
    public TableDto updateTable(Long tableId, TableRequest req) {
        DiningTable t = findTable(tableId);
        t.setLabel(req.label().trim());
        t.setCapacity(req.capacity());
        return TableDto.from(t);
    }

    @Transactional
    public void deactivateTable(Long tableId) {
        findTable(tableId).setActive(false);
    }

    // ---- menu (MongoDB) ----
    @Cacheable(value = "menus", key = "#restaurantId")
    public Menu getMenu(Long restaurantId) {
        return menus.findByRestaurantId(restaurantId).orElseGet(() -> {
            Menu m = new Menu();
            m.setRestaurantId(restaurantId);
            return m;
        });
    }

    @CacheEvict(value = "menus", key = "#restaurantId")
    public Menu saveMenu(Long restaurantId, List<Menu.Section> sections) {
        find(restaurantId);
        Menu menu = menus.findByRestaurantId(restaurantId).orElseGet(() -> {
            Menu m = new Menu();
            m.setRestaurantId(restaurantId);
            return m;
        });
        menu.setSections(sections);
        return menus.save(menu);
    }

    private void apply(Restaurant r, RestaurantRequest req) {
        if (!req.openTime().isBefore(req.closeTime())) {
            throw ApiException.badRequest("Opening time must be before closing time");
        }
        r.setName(req.name().trim());
        r.setDescription(req.description());
        r.setCuisine(req.cuisine().trim());
        r.setAddress(req.address().trim());
        r.setPhone(req.phone());
        r.setOpenTime(req.openTime());
        r.setCloseTime(req.closeTime());
    }

    private Restaurant find(Long id) {
        return restaurants.findById(id).filter(Restaurant::isActive)
                .orElseThrow(() -> ApiException.notFound("Restaurant"));
    }

    private DiningTable findTable(Long id) {
        return tables.findById(id).orElseThrow(() -> ApiException.notFound("Table"));
    }
}
