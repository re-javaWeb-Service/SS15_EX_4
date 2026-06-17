package com.re.bt4.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * HomeController: Các endpoint kiểm tra xác thực
 * 
 * Endpoints:
 * - GET / : Trang chủ, public
 * - GET /login : Trang đăng nhập, public
 * - GET /dashboard : Yêu cầu xác thực
 * - GET /admin : Yêu cầu ROLE_ADMIN
 * - GET /librarian : Yêu cầu ROLE_LIBRARIAN
 * - GET /profile : Yêu cầu xác thực, hiển thị thông tin người dùng hiện tại
 */
@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, String> home() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Chào mừng đến Hệ Thống Quản Lý Thư Viện");
        response.put("info", "Hệ thống sử dụng CustomUserDetailsService + BCryptPasswordEncoder");
        response.put("test_users", "admin/admin123, librarian/libpwd123, john/password123");
        return response;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Chào mừng " + auth.getName());
        response.put("username", auth.getName());
        response.put("roles", auth.getAuthorities().toString());
        response.put("authenticated", auth.isAuthenticated());
        return response;
    }

    @GetMapping("/admin")
    public Map<String, String> adminPage() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Trang quản trị (chỉ ADMIN có thể truy cập)");
        response.put("info", "Bạn có role: ROLE_ADMIN");
        return response;
    }

    @GetMapping("/librarian")
    public Map<String, String> librarianPage() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Trang thủ thư (chỉ LIBRARIAN có thể truy cập)");
        response.put("info", "Bạn có role: ROLE_LIBRARIAN");
        return response;
    }

    @GetMapping("/profile")
    public Map<String, Object> userProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();
        response.put("username", auth.getName());
        response.put("principal", auth.getPrincipal());
        response.put("authorities", auth.getAuthorities());
        response.put("credentials_erased", auth.getCredentials() == null);
        return response;
    }
}
