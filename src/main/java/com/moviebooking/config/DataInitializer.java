package com.moviebooking.config;

import com.moviebooking.model.Room;
import com.moviebooking.model.Theater;
import com.moviebooking.model.User;
import com.moviebooking.model.enums.Gender;
import com.moviebooking.model.enums.Role;
import com.moviebooking.model.enums.RoomType;
import com.moviebooking.repository.RoomRepository;
import com.moviebooking.repository.TheaterRepository;
import com.moviebooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TheaterRepository theaterRepository;
    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        // Schema cleanup for legacy/orphaned columns in products table
        try {
            jdbcTemplate.execute("ALTER TABLE products DROP COLUMN stock");
            log.info("Dropped legacy column 'stock' from products table.");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("ALTER TABLE products DROP COLUMN is_combo");
            log.info("Dropped legacy column 'is_combo' from products table.");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("ALTER TABLE products MODIFY COLUMN category VARCHAR(50) NOT NULL");
            log.info("Updated category column type to VARCHAR(50) in products table.");
        } catch (Exception ignored) {}

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

        // Seed Primary Theater
        Theater primaryTheater;
        if (theaterRepository.count() == 0) {
            primaryTheater = Theater.builder()
                    .name("Trung tâm Chiếu phim Quốc gia")
                    .address("87 Láng Hạ, Ba Đình, Hà Nội")
                    .city("Hà Nội")
                    .district("Ba Đình")
                    .phone("02435141791")
                    .email("neda@chieuphimquocgia.com.vn")
                    .description("Trung tâm Chiếu phim Quốc gia (NCC) - CineMind Primary Center")
                    .isActive(true)
                    .build();
            primaryTheater = theaterRepository.save(primaryTheater);
            log.info("Initialized Primary Theater: {} (ID={})", primaryTheater.getName(), primaryTheater.getId());
        } else {
            primaryTheater = theaterRepository.findAll().get(0);
        }

        // Seed 14 NCC Mapped Rooms
        if (roomRepository.count() == 0 && primaryTheater != null) {
            List<RoomSeedData> seeds = List.of(
                    new RoomSeedData("86", "Phòng chiếu 1"),
                    new RoomSeedData("2100", "Phòng chiếu 2"),
                    new RoomSeedData("2101", "Phòng chiếu 3"),
                    new RoomSeedData("2102", "Phòng chiếu 4"),
                    new RoomSeedData("2103", "Phòng chiếu 5"),
                    new RoomSeedData("2104", "Phòng chiếu 6"),
                    new RoomSeedData("2114", "Phòng chiếu 7"),
                    new RoomSeedData("2115", "Phòng chiếu 8"),
                    new RoomSeedData("2116", "Phòng chiếu 9"),
                    new RoomSeedData("2117", "Phòng chiếu 10"),
                    new RoomSeedData("2119", "Phòng chiếu 11"),
                    new RoomSeedData("2120", "Phòng chiếu 12"),
                    new RoomSeedData("2121", "Phòng chiếu 13"),
                    new RoomSeedData("2122", "Phòng chiếu 14")
            );

            for (RoomSeedData seed : seeds) {
                Room room = Room.builder()
                        .name(seed.name())
                        .roomType(RoomType.TWO_D)
                        .theater(primaryTheater)
                        .sourceRoomId(seed.sourceRoomId())
                        .isActive(true)
                        .build();
                roomRepository.save(room);
            }

            log.info("Initialized 14 NCC-mapped rooms under Primary Theater ID={}", primaryTheater.getId());
        }
    }

    private record RoomSeedData(String sourceRoomId, String name) {}
}

