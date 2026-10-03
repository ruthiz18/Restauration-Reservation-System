package com.reservo.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "restaurants", indexes = @Index(name = "idx_restaurants_cuisine", columnList = "cuisine"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 60)
    private String cuisine;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false)
    private LocalTime openTime;

    @Column(nullable = false)
    private LocalTime closeTime;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
