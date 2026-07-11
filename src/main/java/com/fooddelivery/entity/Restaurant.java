package com.fooddelivery.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "restaurants")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String cuisine;

    @Column(columnDefinition = "double default 0.0")
    private Double rating;

    @Column(name = "image_url")
    private String imageUrl;

    @Column
    private Double latitude;

    @Column
    private Double longitude;
}
