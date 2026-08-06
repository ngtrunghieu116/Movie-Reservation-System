package com.moviebooking.model;

import com.moviebooking.model.enums.Gender;
import com.moviebooking.model.enums.Role;
import com.moviebooking.model.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false, length = 100)
    private String email;
    @Column(nullable = false)
    private String password;
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;
    @Column(unique = true, nullable = false, length = 15)
    private String phone;

    @Column(length = 500)
    private String address;
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = true;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
// Lưu ý: Các quan hệ @OneToMany (với Reservation, Review, AiUsageLog)
// chúng ta sẽ thêm vào sau khi tạo xong các Entity đó ở Bước 6
// để tránh bị lỗi đỏ do chưa tìm thấy class.
