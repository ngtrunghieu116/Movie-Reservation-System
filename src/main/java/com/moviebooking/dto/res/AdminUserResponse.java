package com.moviebooking.dto.res;

import com.moviebooking.model.User;
import com.moviebooking.model.enums.Gender;
import com.moviebooking.model.enums.Role;
import com.moviebooking.model.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate dateOfBirth;
    private Gender gender;
    private Role role;
    private UserStatus status;
    private LocalDateTime createdAt;

    public static AdminUserResponse fromEntity(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .role(user.getRole())
                .status(user.getStatus() != null ? user.getStatus() : UserStatus.ACTIVE)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
