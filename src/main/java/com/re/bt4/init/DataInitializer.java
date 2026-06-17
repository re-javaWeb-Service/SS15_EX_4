package com.re.bt4.init;

import com.re.bt4.entity.User;
import com.re.bt4.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * DataInitializer: Khởi tạo dữ liệu mẫu cho H2 database
 * 
 * Chạy tự động khi Spring Boot khởi động
 * - Kiểm tra xem database đã có user chưa
 * - Nếu chưa, thêm các user mẫu
 * - Mật khẩu được mã hóa bằng BCrypt
 * 
 * Test users:
 * 1. admin / admin123 (roles: ROLE_ADMIN, ROLE_USER)
 * 2. librarian / libpwd123 (roles: ROLE_LIBRARIAN, ROLE_USER)
 * 3. john / password123 (roles: ROLE_USER)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Kiểm tra xem database đã có dữ liệu chưa
        if (userRepository.count() > 0) {
            log.info("Database already contains users, skipping initialization");
            return;
        }

        log.info("Initializing database with sample users...");

        // User 1: Admin
        User admin = new User();
        admin.setUsername("admin");
        // Mã hóa mật khẩu "admin123" bằng BCrypt
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRoles("ROLE_ADMIN, ROLE_USER");
        userRepository.save(admin);
        log.info("Created user: admin with roles: ROLE_ADMIN, ROLE_USER");

        // User 2: Librarian
        User librarian = new User();
        librarian.setUsername("librarian");
        librarian.setPassword(passwordEncoder.encode("libpwd123"));
        librarian.setRoles("ROLE_LIBRARIAN, ROLE_USER");
        userRepository.save(librarian);
        log.info("Created user: librarian with roles: ROLE_LIBRARIAN, ROLE_USER");

        // User 3: Regular User
        User user = new User();
        user.setUsername("john");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRoles("ROLE_USER");
        userRepository.save(user);
        log.info("Created user: john with roles: ROLE_USER");

        log.info("Database initialization completed!");
        log.info("Test credentials:");
        log.info("  Admin: username=admin, password=admin123");
        log.info("  Librarian: username=librarian, password=libpwd123");
        log.info("  User: username=john, password=password123");
    }
}
