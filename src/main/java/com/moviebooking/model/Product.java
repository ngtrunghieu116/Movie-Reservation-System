package com.moviebooking.model;

import com.moviebooking.model.enums.ProductCategory;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProductCategory category;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;
    @Column(name = "image_path", length = 255)
    private String imagePath;
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
}
