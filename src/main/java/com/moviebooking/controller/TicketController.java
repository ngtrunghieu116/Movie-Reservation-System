package com.moviebooking.controller;

import com.moviebooking.dto.req.ValidateTicketRequest;
import com.moviebooking.dto.res.TicketResponse;
import com.moviebooking.model.User;
import com.moviebooking.security.SecurityUtils;
import com.moviebooking.service.booking.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final SecurityUtils securityUtils;

    @GetMapping("/{ticketCode}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable String ticketCode) {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ticketService.getTicketByCode(ticketCode, currentUser));
    }

    @PostMapping("/validate")
    public ResponseEntity<Void> validateTicket(@Valid @RequestBody ValidateTicketRequest request) {
        ticketService.validateTicket(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{ticketCode}/check-in")
    public ResponseEntity<Void> checkInTicket(@PathVariable String ticketCode) {
        ticketService.checkInTicket(ticketCode);
        return ResponseEntity.ok().build();
    }
}
