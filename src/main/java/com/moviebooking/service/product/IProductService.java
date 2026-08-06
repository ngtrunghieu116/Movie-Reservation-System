package com.moviebooking.service.product;

import com.moviebooking.dto.req.ProductRequest;
import com.moviebooking.dto.res.PageResponse;
import com.moviebooking.dto.res.ProductResponse;
import com.moviebooking.model.enums.ProductCategory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IProductService {
    PageResponse<ProductResponse> getAllProducts(ProductCategory category, Boolean isActive, String search, int page, int size, String sort);
    ProductResponse getProductById(Long id);
    ProductResponse createProduct(ProductRequest request, MultipartFile image);
    ProductResponse updateProduct(Long id, ProductRequest request, MultipartFile image);
    void deleteProduct(Long id);
    List<ProductResponse> getPublicProducts();
}
