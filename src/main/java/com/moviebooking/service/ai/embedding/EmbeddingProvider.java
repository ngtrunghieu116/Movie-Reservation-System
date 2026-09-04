package com.moviebooking.service.ai.embedding;

import java.util.List;

/**
 * Abstraction cho nhà cung cấp embedding.
 * Pipeline phụ thuộc vào interface này, không gắn cứng vào một provider cụ thể.
 */
public interface EmbeddingProvider {

    /** Tên nhà cung cấp (mock, gemini, openai) */
    String getProviderName();

    /** Tạo embedding cho một danh sách văn bản */
    List<float[]> embed(List<String> texts);

    /** Số chiều vector */
    int getDimension();
}
