package com.campus.lostfound.controller;

import com.campus.lostfound.domain.User;
import com.campus.lostfound.repo.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public UserProfileController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal String username) {
        return userRepository.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal String username, @RequestBody Map<String, Object> updatedUser) {
        try {
            return userRepository.findByUsername(username).map(user -> {
                if (updatedUser.containsKey("nickname")) user.setNickname((String) updatedUser.get("nickname"));
                if (updatedUser.containsKey("department")) user.setDepartment((String) updatedUser.get("department"));
                if (updatedUser.containsKey("phone")) user.setPhone((String) updatedUser.get("phone"));
                if (updatedUser.containsKey("email")) user.setEmail((String) updatedUser.get("email"));
                if (updatedUser.containsKey("bio")) user.setBio((String) updatedUser.get("bio"));
                userRepository.save(user);
                return ResponseEntity.ok(user);
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "保存失败: " + e.getMessage()));
        }
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@AuthenticationPrincipal String username, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "文件不能为空"));
        
        try {
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir, filename);
            if (!Files.exists(path.getParent())) Files.createDirectories(path.getParent());
            Files.copy(file.getInputStream(), path);
            
            String avatarUrl = "/static/" + filename;
            userRepository.findByUsername(username).ifPresent(user -> {
                user.setAvatar(avatarUrl);
                userRepository.save(user);
            });
            
            return ResponseEntity.ok(Map.of("url", avatarUrl));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "上传失败: " + e.getMessage()));
        }
    }

    @PostMapping("/password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal String username, @RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        
        return userRepository.findByUsername(username).map(user -> {
            if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
                return ResponseEntity.badRequest().body(Map.of("message", "原密码错误"));
            }
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "密码修改成功"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
