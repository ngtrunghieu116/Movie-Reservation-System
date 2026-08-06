package com.moviebooking.dto.res;

import com.moviebooking.model.enums.ProductCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private ProductCategory category;
    private String description;
    private BigDecimal price;
    private Integer availableQuantity;
    private Boolean isActive;
    private Integer displayOrder;
    private String imagePath;
}
