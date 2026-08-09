package com.moviebooking.crawler.resolver;

import com.moviebooking.model.Genre;
import com.moviebooking.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenreResolver {

    private final GenreRepository genreRepository;

    @Transactional
    public Set<Genre> resolve(String rawGenres) {
        if (rawGenres == null || rawGenres.isBlank()) {
            return Collections.emptySet();
        }

        // split by comma, trim, normalize and distinct
        Set<String> normalizedGenreNames = Arrays.stream(rawGenres.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::normalize)
                .collect(Collectors.toSet());

        Set<Genre> resolvedGenres = new HashSet<>();

        for (String name : normalizedGenreNames) {
            Genre genre = genreRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> {
                        log.info("Creating new genre: {}", name);
                        Genre newGenre = new Genre();
                        newGenre.setName(name);
                        return genreRepository.save(newGenre);
                    });
            resolvedGenres.add(genre);
        }

        return resolvedGenres;
    }

    private String normalize(String name) {
        if (name == null || name.isEmpty()) return name;
        // Format: "hành động" -> "Hành động"
        String lower = name.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
