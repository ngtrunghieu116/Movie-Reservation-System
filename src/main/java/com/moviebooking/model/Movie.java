package com.moviebooking.model;
import com.moviebooking.model.enums.AgeRating;
import com.moviebooking.model.enums.MovieStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "movies")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(name = "title_en", length = 200)
    private String titleEn;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false, length = 200)
    private String director;
    @Column(nullable = false, length = 500)
    private String actors;
    @Column(nullable = false)
    private Integer duration;
    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    @Column(name = "poster_path", nullable = false, length = 255)
    private String posterPath;
    @Column(name = "banner_path", length = 255)
    private String bannerPath;
    @Column(name = "trailer_url", length = 500)
    private String trailerUrl;
    @Enumerated(EnumType.STRING)
    @Column(name = "age_rating", nullable = false)
    private AgeRating ageRating;
    @Column(nullable = false, length = 100)
    private String language;
    @Column(length = 100)
    private String subtitle;

    // Crawler fields
    @Column(length = 50)
    private String source;
    @Column(name = "source_id", length = 100, unique = true)
    private String sourceId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovieStatus status;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "movie_genres",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();
}
