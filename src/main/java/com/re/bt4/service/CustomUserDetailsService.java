package com.re.bt4.service;

import com.re.bt4.entity.User;
import com.re.bt4.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CustomUserDetailsService: Tải thông tin người dùng từ Database
 * 
 * Vai trò:
 * - Kết nối Spring Security với cơ sở dữ liệu người dùng
 * - Truy vấn thông tin người dùng dựa trên username
 * - Xây dựng UserDetails với authorities từ roles trong DB
 * - Cho phép xác thực người dùng một cách động, không cần khởi động lại
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Tải thông tin người dùng từ database
     * 
     * @param username tên đăng nhập của người dùng
     * @return UserDetails chứa username, password (đã mã hóa), và authorities
     * @throws UsernameNotFoundException nếu người dùng không tồn tại
     * 
     * Quy trình:
     * 1. Truy vấn database tìm user theo username
     * 2. Nếu không tìm thấy → throw exception
     * 3. Parse roles (CSV format) thành GrantedAuthority list
     * 4. Xây dựng Spring UserDetails object
     * 5. Return cho AuthenticationManager để xác minh mật khẩu
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Bước 1: Truy vấn database
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "Không tìm thấy người dùng: " + username));

        // Bước 2: Chuyển đổi roles (định dạng: "ROLE_USER, ROLE_ADMIN") thành GrantedAuthority
        List<GrantedAuthority> authorities = parseRoles(user.getRoles());

        // Bước 3: Xây dựng UserDetails
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),           // username
            user.getPassword(),           // password đã mã hóa từ DB
            true,                         // enabled
            true,                         // accountNonExpired
            true,                         // credentialsNonExpired
            true,                         // accountNonLocked
            authorities                   // danh sách quyền hạn
        );
    }

    /**
     * Chuyển đổi chuỗi roles thành danh sách GrantedAuthority
     * 
     * Định dạng input: "ROLE_USER, ROLE_ADMIN, ROLE_LIBRARIAN"
     * Output: List[SimpleGrantedAuthority("ROLE_USER"), ...]
     */
    private List<GrantedAuthority> parseRoles(String rolesString) {
        if (rolesString == null || rolesString.trim().isEmpty()) {
            return List.of();
        }

        return Arrays.stream(rolesString.split(","))
            .map(String::trim)
            .filter(role -> !role.isEmpty())
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }
}
