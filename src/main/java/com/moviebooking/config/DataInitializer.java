package com.moviebooking.config;

import com.moviebooking.model.User;
import com.moviebooking.model.enums.Gender;
import com.moviebooking.model.enums.Role;
import com.moviebooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@gmail.com")) {
            User admin = User.builder()
                    .email("admin@gmail.com")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("Admin")
                    .lastName("System")
                    .phone("0900000000")
                    .dateOfBirth(LocalDate.of(1995, 1, 1))
                    .gender(Gender.MALE)
                    .role(Role.ADMIN)
                    .emailVerified(true)
                    .build();
            userRepository.save(admin);
            log.info("Initialized default Admin account: admin@gmail.com / admin123");
        }
    }
}
