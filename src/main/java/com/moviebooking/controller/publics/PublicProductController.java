package com.moviebooking.controller.publics;

import com.moviebooking.dto.res.ProductResponse;
import com.moviebooking.service.product.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class PublicProductController {

    private final IProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getPublicProducts() {
        return ResponseEntity.ok(productService.getPublicProducts());
    }
}
