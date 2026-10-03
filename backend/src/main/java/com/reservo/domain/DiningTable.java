package com.reservo.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dining_tables",
        uniqueConstraints = @UniqueConstraint(name = "uk_table_label", columnNames = {"restaurant_id", "label"}),
        indexes = @Index(name = "idx_tables_restaurant", columnList = "restaurant_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DiningTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false, length = 30)
    private String label;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
