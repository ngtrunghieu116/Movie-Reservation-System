package com.moviebooking.service.user;

import com.moviebooking.dto.req.UpdateProfileRequest;
import com.moviebooking.dto.res.TransactionHistoryResponse;
import com.moviebooking.dto.res.UserProfileResponse;
import com.moviebooking.model.Reservation;
import com.moviebooking.model.User;
import com.moviebooking.repository.ReservationRepository;
import com.moviebooking.repository.ReservedSeatRepository;
import com.moviebooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final ReservedSeatRepository reservedSeatRepository;

    @Override
    public UserProfileResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản!"));

        return mapToResponse(user);
    }

    @Override
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản!"));

        if (!user.getPhone().equals(request.getPhone()) && userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Số điện thoại này đã được sử dụng bởi tài khoản khác!");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());

        userRepository.save(user);
        return mapToResponse(user);
    }

    @Override
    public List<TransactionHistoryResponse> getTransactionHistory(String email) {
        // Query transactions belonging to user
        List<Reservation> reservations = reservationRepository.findByUserEmailOrderByCreatedAtDesc(email);

        return reservations.stream().map(reservation -> {
            int ticketCount = reservedSeatRepository.countByReservationId(reservation.getId());
            return TransactionHistoryResponse.builder()
                    .transactionDate(reservation.getCreatedAt())
                    .movieTitle(reservation.getShowtime().getMovie().getTitle())
                    .transactionType("Đặt vé phim")
                    .ticketCount(ticketCount)
                    .totalAmount(reservation.getTotalPrice())
                    .build();
        }).collect(Collectors.toList());
    }

    private UserProfileResponse mapToResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .role(user.getRole().name())
                .build();
    }
}
