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
import com.moviebooking.dto.req.AdminUserRequest;
import com.moviebooking.dto.res.AdminUserResponse;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.model.enums.Role;
import com.moviebooking.model.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản!"));

        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản!"));

        if (!user.getPhone().equals(request.getPhone()) && userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại này đã được sử dụng bởi tài khoản khác!");
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
    @Transactional(readOnly = true)
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

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> searchUsers(Role role, UserStatus status, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String searchPattern = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        return userRepository.searchUsers(role, status, searchPattern, pageable)
                .map(AdminUserResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng!"));
        return AdminUserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public AdminUserResponse adminUpdateUser(Long id, AdminUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng!"));

        if (!user.getPhone().equals(request.getPhone()) && userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại này đã được sử dụng bởi tài khoản khác!");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus());

        User saved = userRepository.save(user);
        return AdminUserResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void adminResetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng!"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng!"));

        boolean hasReservations = reservationRepository.existsByUserId(id);
        if (hasReservations) {
            throw new IllegalArgumentException(
                    "Không thể xóa tài khoản này do đã có lịch sử đặt vé! Bạn có thể chuyển trạng thái sang ĐÃ KHÓA (BLOCKED) để vô hiệu hóa tài khoản.");
        }

        userRepository.delete(user);
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
