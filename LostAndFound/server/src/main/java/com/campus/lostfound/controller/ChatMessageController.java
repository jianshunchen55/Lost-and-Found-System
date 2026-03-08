package com.campus.lostfound.controller;

import com.campus.lostfound.repo.UserRepository;
import com.campus.lostfound.service.ChatMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatMessageController {
    private final ChatMessageService service;
    private final UserRepository userRepository;

    public ChatMessageController(ChatMessageService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@AuthenticationPrincipal String username, @RequestBody Map<String, Object> body) {
        return userRepository.findByUsername(username).map(user -> {
            Long receiverId = Long.valueOf(body.get("receiverId").toString());
            String content = (String) body.get("content");
            service.sendMessage(user.getId(), receiverId, content);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.status(401).build());
    }

    @GetMapping("/received")
    public ResponseEntity<?> getReceived(@AuthenticationPrincipal String username, 
                                       @RequestParam(defaultValue = "0") int page, 
                                       @RequestParam(defaultValue = "10") int size) {
        return userRepository.findByUsername(username).map(user -> {
            return ResponseEntity.ok(service.getReceivedMessages(user.getId(), page, size));
        }).orElse(ResponseEntity.status(401).build());
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@AuthenticationPrincipal String username, @RequestParam Long targetId) {
        return userRepository.findByUsername(username).map(user -> {
            return ResponseEntity.ok(service.getHistory(user.getId(), targetId));
        }).orElse(ResponseEntity.status(401).build());
    }

    @PutMapping("/read/{id}")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        service.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-from/{senderId}")
    public ResponseEntity<?> markAsReadFrom(@AuthenticationPrincipal String username, @PathVariable Long senderId) {
        return userRepository.findByUsername(username).map(user -> {
            service.markAsReadFrom(user.getId(), senderId);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.status(401).build());
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@AuthenticationPrincipal String username) {
        return userRepository.findByUsername(username).map(user -> {
            service.markAllAsRead(user.getId());
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.status(401).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.deleteMessage(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAll(@AuthenticationPrincipal String username) {
        return userRepository.findByUsername(username).map(user -> {
            service.deleteAll(user.getId());
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.status(401).build());
    }
}
