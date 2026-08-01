package com.moviebooking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
@Entity
@Table(name = "movie_embeddings")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MovieEmbedding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false, unique = true)
    private Movie movie;
    @Column(name = "embedding_vector", nullable = false, columnDefinition = "JSON")
    private String embeddingVector;
    @Column(name = "movie_text", nullable = false, columnDefinition = "TEXT")
    private String movieText;
    @Column(name = "model_name", nullable = false, length = 50)
    private String modelName = "text-embedding-3-small";
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
