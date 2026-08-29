package com.moviebooking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.moviebooking.exception.EmailNotVerifiedException;
import com.moviebooking.exception.DuplicateResourceException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        // Lọc ra tất cả các trường bị lỗi và tin báo lỗi tương ứng
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // Trả về mã 400 (Bad Request)
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    // 2. Xử lý lỗi logic (Ví dụ: Email đã tồn tại) mà ta chủ động throw
    // RuntimeException
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeExceptions(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateResourceException(DuplicateResourceException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(
            BadCredentialsException ex) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "message",
                        "Tài khoản hoặc mật khẩu không đúng."));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, String>> handleEmailNotVerified(
            EmailNotVerifiedException ex) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "message",
                        ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "message",
                        "Dữ liệu đã tồn tại hoặc vi phạm ràng buộc hệ thống. Vui lòng kiểm tra lại!"));
    }

    @ExceptionHandler(SeatAlreadyReservedException.class)
    public ResponseEntity<Map<String, String>> handleSeatAlreadyReserved(SeatAlreadyReservedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidSeatHoldException.class)
    public ResponseEntity<Map<String, String>> handleInvalidSeatHold(InvalidSeatHoldException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(SeatHoldOwnershipException.class)
    public ResponseEntity<Map<String, String>> handleSeatHoldOwnership(SeatHoldOwnershipException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ShowtimeNotBookableException.class)
    public ResponseEntity<Map<String, String>> handleShowtimeNotBookable(ShowtimeNotBookableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ProductInactiveException.class)
    public ResponseEntity<Map<String, String>> handleProductInactive(ProductInactiveException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidQuantityException.class)
    public ResponseEntity<Map<String, String>> handleInvalidQuantity(InvalidQuantityException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ReservationNotModifiableException.class)
    public ResponseEntity<Map<String, String>> handleReservationNotModifiable(ReservationNotModifiableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientInventory(InsufficientInventoryException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleTicketNotFound(TicketNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(TicketAlreadyCheckedInException.class)
    public ResponseEntity<Map<String, String>> handleTicketAlreadyCheckedIn(TicketAlreadyCheckedInException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(TicketValidationException.class)
    public ResponseEntity<Map<String, String>> handleTicketValidation(TicketValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedTicketAccessException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorizedTicketAccess(UnauthorizedTicketAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", ex.getMessage()));
    }


    // 5. Xử lý tất cả các lỗi không mong muốn khác (System Error / 500)
    // Giúp bảo vệ hệ thống, log lỗi chi tiết ra console nhưng chỉ trả về thông báo
    // thân thiện cho User
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralExceptions(Exception ex) {
        ex.printStackTrace(); // Log full stack trace để dev debug
        Map<String, String> error = new HashMap<>();
        error.put("message", "Hệ thống gặp sự cố ngoài dự kiến. Vui lòng thử lại sau!");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

