package com.moviebooking.controller;

import com.moviebooking.dto.res.TheaterResponse;
import com.moviebooking.service.theater.ITheaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/theaters")
@RequiredArgsConstructor
public class PublicTheaterController {

    private final ITheaterService theaterService;

    @GetMapping
    public ResponseEntity<List<TheaterResponse>> getAllActiveTheaters() {
        return ResponseEntity.ok(theaterService.getAllActiveTheaters());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TheaterResponse> getTheaterById(@PathVariable Long id) {
        return ResponseEntity.ok(theaterService.getTheaterById(id));
    }
}
