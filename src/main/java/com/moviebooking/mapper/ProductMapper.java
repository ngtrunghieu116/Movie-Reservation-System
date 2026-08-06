package com.moviebooking.mapper;

import com.moviebooking.dto.req.ProductRequest;
import com.moviebooking.dto.res.ProductResponse;
import com.moviebooking.model.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .description(product.getDescription())
                .price(product.getPrice())
                .availableQuantity(product.getAvailableQuantity())
                .isActive(product.getIsActive())
                .displayOrder(product.getDisplayOrder())
                .imagePath(product.getImagePath())
                .build();
    }

    public Product toEntity(ProductRequest request) {
        if (request == null) {
            return null;
        }
        return Product.builder()
                .name(request.getName())
                .category(request.getCategory())
                .description(request.getDescription())
                .price(request.getPrice())
                .availableQuantity(request.getAvailableQuantity())
                .isActive(request.getIsActive())
                .displayOrder(request.getDisplayOrder())
                .build();
    }

    public void updateEntity(Product product, ProductRequest request) {
        if (product == null || request == null) {
            return;
        }
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setAvailableQuantity(request.getAvailableQuantity());
        product.setIsActive(request.getIsActive());
        product.setDisplayOrder(request.getDisplayOrder());
    }
}
