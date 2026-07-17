package com.fooddelivery.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meal_plans")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type; // e.g. Veg Meal, Protein Meal, Family Meal

    @Column(nullable = false)
    private Double price;

    @Column(length = 1000)
    private String description;
}
