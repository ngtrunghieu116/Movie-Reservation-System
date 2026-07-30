package com.moviebooking.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;


@Entity
@Table(name = "theaters")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Theater {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 200)
    private String address;
    @Column(nullable = false, length = 100)
    private String city;
    @Column(nullable = false, length = 100)
    private String district;
    @Column(nullable = false, length = 15)
    private String phone;
    @Column(length = 100)
    private String email;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
