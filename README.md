# Bài Tập 4: Quản Lý Người Dùng Thư Viện - CustomUserDetailsService & PasswordEncoder

## 📋 Phần 1: Phân Tích Logic

### 1.1 Vai Trò Của UserDetailsService Trong Spring Security

#### Khái Niệm Cơ Bản

`UserDetailsService` là một interface cốt lõi trong Spring Security, đóng vai trò như "cầu nối" giữa hệ thống xác thực (Authentication) và nguồn dữ liệu người dùng (database). Nó chịu trách nhiệm:

1. **Truy xuất thông tin người dùng**: Lấy dữ liệu người dùng từ database (hoặc bất kỳ nguồn nào) dựa trên username
2. **Xây dựng User Details**: Tạo đối tượng `UserDetails` chứa username, password (đã mã hóa), và authorities (quyền hạn)
3. **Cấp quyền động**: Cho phép quản lý quyền hạn từ database, không cần khởi động lại ứng dụng

#### Luồng Xác Thực Với UserDetailsService

```
1. User gửi thông tin đăng nhập (username, password)
   ↓
2. AuthenticationProvider gọi UserDetailsService.loadUserByUsername(username)
   ↓
3. UserDetailsService truy vấn database, trả về UserDetails
   ↓
4. PasswordEncoder so sánh mật khẩu người dùng gửi với mật khẩu đã mã hóa
   ↓
5. Nếu trùng khớp → Tạo Authentication object và lưu vào SecurityContext
   ↓
6. Truy cập được bảo vệ theo quyền hạn trong Authorities
```

### 1.2 Vai Trò Của PasswordEncoder

#### Tại Sao Cần PasswordEncoder?

PasswordEncoder là thành phần chịu trách nhiệm:

1. **Mã hóa mật khẩu**: Biến đổi mật khẩu plain text thành hash không thể đảo ngược
2. **Xác minh mật khẩu**: So sánh mật khẩu người dùng nhập với hash đã lưu
3. **Bảo vệ dữ liệu**: Nếu database bị xâm phạm, mật khẩu vẫn an toàn

#### Các Loại PasswordEncoder

- `NoOpPasswordEncoder`: ❌ **KHÔNG DÙNG** - không mã hóa, cực kỳ nguy hiểm
- `StandardPasswordEncoder`: ❌ Lỗi thời, không an toàn
- `Pbkdf2PasswordEncoder`: ⚠️ Có thể, nhưng không phải tốt nhất
- `bcryptPasswordEncoder`: ✅ **KHUYẾN NGHỊ** - an toàn, thích ứng với công nghệ
- `argon2PasswordEncoder`: ✅ Tối ưu nhất, nhưng chậm hơn bcrypt

### 1.3 Tại Sao Plain Text & NoOpPasswordEncoder Nguy Hiểm?

#### Szenario Thực Tế - Kinh Hoàng Khi Bị Vi Phạm

```
❌ Plain Text Password Storage:
   Database bị hack → Tất cả mật khẩu hiếu lộ
   Mỗi tài khoản bị chiếm trong vòng vài phút
   User có thể bị hack ở các ứng dụng khác (reuse password)

❌ NoOpPasswordEncoder:
   Input: password="admin123"
   Output: admin123 (không thay đổi!)
   Kết quả: Bảo mật = 0
```

#### Rủi Ro Cụ Thể

1. **Rainbow Table Attack**: Hacker sử dụng hash table có sẵn để so khớp mật khẩu
2. **Brute Force Attack**: Thử từng mật khẩu cho đến khi tìm thấy
3. **Dictionary Attack**: Sử dụng danh sách mật khẩu phổ biến
4. **Compliance Violations**: Vi phạm GDPR, PCI DSS, và các quy định bảo mật
5. **Loss of Trust**: Nếu bị lộ, người dùng sẽ mất tin tưởng vào hệ thống

### 1.4 Tại Sao BCryptPasswordEncoder Là Lựa Chọn Tối Ưu?

#### Đặc Điểm BCrypt

```
✅ Adaptive Hashing: Tăng độ phức tạp khi máy tính nhanh hơn
✅ Salting: Thêm ngẫu nhiên vào hash, ngăn Rainbow Table
✅ Slow by Design: Mỗi mã hóa mất ~100ms, làm chậm brute force
✅ Các tin Chuẩn: được khuyến nghị bởi OWASP, Spring Security
```

#### Ví Dụ So Sánh

```
Input: "password123"

Plain Text:
  Result: password123
  Time: 0.001ms
  Crack time: <1s (với GPU modern)

NoOpPasswordEncoder:
  Result: password123
  Time: 0.001ms
  Crack time: <1s

BCrypt (strength=12):
  Result: $2a$12$R9h7cIPz0gi.URNNGLUA2OPST9/PgBkqquzi.Ss7KIUgO2t0jKm2
  Time: 100ms
  Crack time: Hàng năm (với GPU modern)
```

#### Tính Năng Nâng Cao

- **Strength factor**: Tăng từ 10→12→14... làm việc chậm lại, tăng bảo mật
- **Adaptive**: Khi BCrypt được phá vỡ, tăng strength factor lên
- **Backward compatible**: Có thể xác minh mật khẩu ngay cả khi strength thay đổi

---

## 🏗️ Phần 2: Kiến Trúc & Luồng Xác Thực

### 2.1 Sơ Đồ Tương Tác Giữa Các Thành Phần

```
┌─────────────────────────────────────────────────────────────────────┐
│                    USER REQUEST (POST /login)                       │
│                 {username: "john", password: "xxx"}                 │
└─────────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────────┐
│            Spring Security Filter Chain                             │
│  (AuthenticationFilter → AuthenticationManager)                     │
└─────────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────────┐
│        Create Authentication Token                                  │
│  UsernamePasswordAuthenticationToken                                │
│  (username: "john", credentials: "xxx", authorities: [])           │
└─────────────────────────────────────────────────────────────────────┘
                            ↓
                   ┌────────────────┐
                   │ Authentication │
                   │    Manager     │
                   └────────────────┘
                            ↓
      ┌─────────────────────┴────────────────────────┐
      ↓                                               ↓
┌──────────────────────┐                    ┌──────────────────────────┐
│    DaoAuth           │                    │  Custom               │
│    Provider          │                    │  UserDetailsService   │
│                      │                    │  (loadUserByUsername) │
│ 1. Gọi loadUser      │──────────────────→ │                      │
│ 2. Nhận UserDetails  │                    │ 1. Query Database    │
│ 3. So sánh pwd       │ ←──────────────────│ 2. Build UserDetails │
│                      │                    │ 3. Return            │
└──────────────────────┘                    └──────────────────────────┘
      ↓
      │ UserDetails + pwd từ user
      ↓
┌──────────────────────────────────┐
│   PasswordEncoder (BCrypt)       │
│  matches(rawPassword, encoded)   │
│                                  │
│ if hash(rawPwd) == encodedPwd    │
│    → return true                 │
│ else                             │
│    → return false                │
└──────────────────────────────────┘
      ↓ Password matched = true
      ↓
┌──────────────────────────────────┐
│  Build Authenticated Token       │
│  (username, pwd, authorities)    │
│  Mark as authenticated = true    │
└──────────────────────────────────┘
      ↓
┌──────────────────────────────────┐
│  SecurityContextHolder.setContext│
│  (SecurityContext)               │
└──────────────────────────────────┘
      ↓
┌──────────────────────────────────┐
│  Response: 200 OK + JWT Token    │
│  (hoặc Session Cookie)           │
└──────────────────────────────────┘
```

### 2.2 Luồng Database Truy Vấn Chi Tiết

```
CustomUserDetailsService
    ↓
    1. loadUserByUsername("john")
    ↓
┌────────────────────────────────────────────┐
│ UserRepository.findByUsername("john")      │
│ ↓                                          │
│ SELECT * FROM user WHERE username = ?     │
│ ↓                                          │
│ User(id=1, username="john",                │
│      password="$2a$12$R9h...", roles="...") │
└────────────────────────────────────────────┘
    ↓
    2. Xây dựng UserDetails từ User entity
    ↓
┌────────────────────────────────────────────┐
│ UserDetails = new User(                    │
│   username: "john",                        │
│   password: "$2a$12$R9h...",               │
│   authorities: [                           │
│     new SimpleGrantedAuthority("ROLE_USER"),│
│     new SimpleGrantedAuthority("ROLE_LIB") │
│   ]                                        │
│ )                                          │
└────────────────────────────────────────────┘
    ↓
    3. Return UserDetails cho AuthenticationManager
```

### 2.3 Sequence Diagram Chi Tiết

```
Client                Spring Security           CustomUserDetailsService
  │                         │                            │
  │─── POST /login ────────→│                            │
  │   {user,pwd}            │                            │
  │                         │                            │
  │                    AuthenticationManager             │
  │                         │                            │
  │                  DaoAuthenticationProvider           │
  │                         │                            │
  │                         │──→ loadUserByUsername()──→│
  │                         │                            │
  │                         │  ┌──────────────────────┐  │
  │                         │  │ Database Query       │  │
  │                         │  │ Find User by name    │  │
  │                         │  └──────────────────────┘  │
  │                         │                            │
  │                         │←─── return UserDetails ───│
  │                         │                            │
  │                    PasswordEncoder                   │
  │                    (BCrypt)                          │
  │                         │                            │
  │                    matches(                          │
  │                    user_input_pwd,                   │
  │                    stored_hash)                      │
  │                         │                            │
  │                    [validate hash]                   │
  │                         │                            │
  │←─── 200 + JWT Token ───│                            │
  │                         │                            │
  │─── GET /resource ──────→│                            │
  │   Bearer: JWT           │                            │
  │                         │ [verify JWT]               │
  │                         │                            │
  │←─── 200 + Resource ────│                            │
```

---

## 🔧 Phần 3: Thực Thi

### 3.1 CustomUserDetailsService Implementation

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        // 1. Truy vấn database
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User không tìm thấy: " + username));

        // 2. Parse roles từ string → authorities
        List<GrantedAuthority> authorities = Arrays.stream(
            user.getRoles().split(","))
            .map(role -> new SimpleGrantedAuthority(role.trim()))
            .collect(Collectors.toList());

        // 3. Tạo UserDetails từ User entity
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),  // Password đã mã hóa từ DB
            authorities
        );
    }
}
```

### 3.2 SecurityConfig Với BCryptPasswordEncoder

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // strength=12: mất ~100ms để mã hóa (balanced: security + performance)
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/login", "/register").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/librarian/**").hasRole("LIBRARIAN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
            );
        return http.build();
    }
}
```

---

## 🎯 Kết Luận

### Điểm Chính:

1. ✅ **UserDetailsService**: Cầu nối giữa Spring Security & Database
2. ✅ **PasswordEncoder**: Bảo vệ mật khẩu bằng hashing & salting
3. ✅ **BCrypt**: Giải pháp tối ưu, adaptive, an toàn
4. ❌ **Tránh**: Plain text, NoOp encoding
5. ✅ **Benefit**: Quản lý người dùng linh hoạt, bảo mật cao, tuân thủ tiêu chuẩn

### Lợi Ích Của Giải Pháp:

- 🔒 **Bảo mật**: Mật khẩu được hash an toàn với BCrypt
- 📊 **Linh hoạt**: Thêm/sửa/xóa người dùng không cần khởi động lại
- 👥 **Đa vai trò**: Hỗ trợ multiple roles/authorities cho mỗi user
- 🔄 **Scalable**: Dễ dàng mở rộng với các hình thức xác thực khác
- 📋 **Compliant**: Tuân thủ các tiêu chuẩn bảo mật hiện đại
"# SS15_EX_4" 
