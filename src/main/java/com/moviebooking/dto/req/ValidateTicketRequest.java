package com.moviebooking.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidateTicketRequest {
    @NotBlank(message = "Ticket code is required")
    private String ticketCode;
}
