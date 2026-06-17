package com.re.bt4.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig: Cấu hình Spring Security
 * 
 * Thành phần chính:
 * 1. PasswordEncoder (BCrypt): Mã hóa mật khẩu an toàn
 * 2. DaoAuthenticationProvider: Xác thực dựa vào database
 * 3. AuthenticationManager: Quản lý quá trình xác thực
 * 4. SecurityFilterChain: Định cấu hình luật truy cập HTTP
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    /**
     * PasswordEncoder Bean: BCryptPasswordEncoder
     * 
     * Tại sao BCrypt:
     * - Adaptive: Tăng độ khó theo thời gian
     * - Salted: Ngăn chặn Rainbow Table attack
     * - Slow: ~100ms/encoding, ngăn brute force
     * - Industry standard: Khuyến nghị bởi OWASP & Spring
     * 
     * strength=12: Cân bằng giữa bảo mật (cao) và performance (có chấp nhận được)
     * Range: 4-31 (cao hơn = an toàn hơn nhưng chậm hơn)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt strength factor: 12 (mất ~100-150ms để mã hóa 1 lần)
        // 10: ~10ms (yếu), 12: ~100ms (tốt), 14: ~1s (rất an toàn)
        return new BCryptPasswordEncoder(12);
    }

    /**
     * DaoAuthenticationProvider: Xác thực từ Database
     * 
     * Quy trình:
     * 1. Nhận UsernamePasswordAuthenticationToken từ filter
     * 2. Gọi CustomUserDetailsService.loadUserByUsername()
     * 3. So sánh mật khẩu input với hash trong DB bằng PasswordEncoder
     * 4. Nếu khớp → trả về authenticated token
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager: Quản lý providers xác thực
     * Sử dụng DaoAuthenticationProvider cho xác thực database
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) 
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * SecurityFilterChain: Cấu hình luật truy cập HTTP
     * 
     * Quy tắc:
     * - Public: /, /login, /register
     * - ADMIN only: /admin/**
     * - LIBRARIAN only: /librarian/**
     * - Authenticated: Mọi yêu cầu khác cần xác thực
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Tắt CSRF cho REST API (enable cho web form)
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/", "/login", "/register", "/api/public/**").permitAll()
                
                // Admin endpoints - yêu cầu ROLE_ADMIN
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                
                // Librarian endpoints - yêu cầu ROLE_LIBRARIAN
                .requestMatchers("/librarian/**", "/api/librarian/**").hasRole("LIBRARIAN")
                
                // Mọi request khác cần xác thực
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }
}

