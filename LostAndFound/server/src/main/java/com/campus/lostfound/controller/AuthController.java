package com.campus.lostfound.controller;

import com.campus.lostfound.domain.Role;
import com.campus.lostfound.domain.User;
import com.campus.lostfound.repo.RoleRepository;
import com.campus.lostfound.repo.UserRepository;
import com.campus.lostfound.util.JwtUtil;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "账号不存在"));
        }

        User user = userOpt.get();
        if (passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            String token = jwtUtil.generateToken(user.getUsername());
            // Simple refresh token implementation: same as access token or separate
            String refreshToken = jwtUtil.generateToken(user.getUsername()); 
            return ResponseEntity.ok(Map.of(
                "accessToken", token,
                "refreshToken", refreshToken,
                "user", user
            ));
        }
        
        return ResponseEntity.status(401).body(Map.of("message", "账号或密码错误"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名已存在"));
        }
        
        // Check adminKey for special roles
        String requestedRoleCode = request.getRole() != null ? request.getRole() : "ROLE_STUDENT";
        if ("ROLE_ADMIN".equals(requestedRoleCode) || "ROLE_MANAGER".equals(requestedRoleCode)) {
            // Simple hardcoded check, in production use config or env var
            if (!"11111".equals(request.getAdminKey())) {
                return ResponseEntity.badRequest().body(Map.of("message", "授权码错误"));
            }
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        
        // Assign role
        Role role = roleRepository.findByCode(requestedRoleCode)
            .orElseGet(() -> {
                Role r = new Role();
                r.setCode(requestedRoleCode);
                r.setName(getNameForRole(requestedRoleCode));
                return roleRepository.save(r);
            });
        user.getRoles().add(role);
        
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "注册成功"));
    }
    
    private String getNameForRole(String code) {
        switch (code) {
            case "ROLE_ADMIN": return "管理员";
            case "ROLE_MANAGER": return "负责人";
            default: return "学生";
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestParam String refreshToken) {
        // Simplified refresh logic
        try {
            String username = jwtUtil.extractUsername(refreshToken);
            if (username != null && !jwtUtil.isTokenExpired(refreshToken)) {
                String newToken = jwtUtil.generateToken(username);
                return ResponseEntity.ok(Map.of("accessToken", newToken));
            }
        } catch (Exception e) {}
        return ResponseEntity.status(401).build();
    }

    @Data
    static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    static class RegisterRequest {
        private String username;
        private String password;
        private String email;
        private String role;
        private String adminKey;
    }
}
