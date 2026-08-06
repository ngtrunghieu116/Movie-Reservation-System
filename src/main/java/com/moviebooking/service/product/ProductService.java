package com.moviebooking.service.product;

import com.moviebooking.dto.req.ProductRequest;
import com.moviebooking.dto.res.PageResponse;
import com.moviebooking.dto.res.ProductResponse;
import com.moviebooking.exception.DuplicateResourceException;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.mapper.ProductMapper;
import com.moviebooking.model.Product;
import com.moviebooking.model.enums.ProductCategory;
import com.moviebooking.repository.ProductRepository;
import com.moviebooking.service.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(ProductCategory category, Boolean isActive, String search, int page, int size, String sort) {
        Sort sortObj = Sort.by(Sort.Direction.DESC, "id");
        if (sort != null && !sort.trim().isEmpty()) {
            String[] sortParams = sort.split(",");
            if (sortParams.length == 2) {
                String sortBy = sortParams[0];
                String sortDir = sortParams[1];
                sortObj = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            }
        }

        if (search != null && search.trim().isEmpty()) {
            search = null;
        } else if (search != null) {
            search = search.trim();
        }

        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<Product> productPage = productRepository.searchProducts(category, isActive, search, pageable);

        List<ProductResponse> content = productPage.getContent().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<ProductResponse>builder()
                .content(content)
                .pageNo(productPage.getNumber())
                .pageSize(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .last(productPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request, MultipartFile image) {
        String trimmedName = request.getName() != null ? request.getName().trim() : "";
        request.setName(trimmedName);

        if (productRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateResourceException("Tên sản phẩm đã tồn tại trong hệ thống.");
        }

        Product product = productMapper.toEntity(request);
        String newImagePath = null;

        if (image != null && !image.isEmpty()) {
            newImagePath = fileStorageService.storeProductImage(image);
            product.setImagePath(newImagePath);
        }

        try {
            Product savedProduct = productRepository.save(product);
            return productMapper.toResponse(savedProduct);
        } catch (Exception ex) {
            // Clean up newly uploaded image if DB save fails
            if (newImagePath != null) {
                fileStorageService.deleteFile(newImagePath);
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, MultipartFile image) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        String trimmedName = request.getName() != null ? request.getName().trim() : "";
        request.setName(trimmedName);

        if (productRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new DuplicateResourceException("Tên sản phẩm này đã trùng với một sản phẩm khác.");
        }

        String oldImagePath = product.getImagePath();
        String newImagePath = null;

        productMapper.updateEntity(product, request);

        if (image != null && !image.isEmpty()) {
            newImagePath = fileStorageService.storeProductImage(image);
            product.setImagePath(newImagePath);
        }

        try {
            Product updatedProduct = productRepository.save(product);
            // Delete old file only after DB save succeeds
            if (newImagePath != null && oldImagePath != null) {
                fileStorageService.deleteFile(oldImagePath);
            }
            return productMapper.toResponse(updatedProduct);
        } catch (Exception ex) {
            // Clean up newly uploaded image if DB save fails
            if (newImagePath != null) {
                fileStorageService.deleteFile(newImagePath);
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        
        // Soft delete
        product.setIsActive(false);
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getPublicProducts() {
        List<Product> products = productRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return products.stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }
}
