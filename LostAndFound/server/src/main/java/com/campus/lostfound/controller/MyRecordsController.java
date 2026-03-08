package com.campus.lostfound.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.domain.User;
import com.campus.lostfound.repo.UserRepository;
import com.campus.lostfound.mapper.MessageMapper;
import com.campus.lostfound.entity.Message;
import com.campus.lostfound.service.LostFoundService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/my")
public class MyRecordsController {

    private final LostFoundService service;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;

    public MyRecordsController(LostFoundService service, UserRepository userRepository, MessageMapper messageMapper) {
        this.service = service;
        this.userRepository = userRepository;
        this.messageMapper = messageMapper;
    }

    @GetMapping("/lost")
    public ResponseEntity<?> getMyLostItems(@AuthenticationPrincipal String username, 
                                            @RequestParam(defaultValue = "0") int page, 
                                            @RequestParam(defaultValue = "10") int size) {
        System.out.println("DEBUG: getMyLostItems called for username: " + username);
        User user = userRepository.findByUsername(username).orElseThrow();
        System.out.println("DEBUG: User ID: " + user.getId());
        Page<com.campus.lostfound.entity.LostItem> items = service.getUserLostItems(user.getId(), page, size);
        System.out.println("DEBUG: Found " + items.getTotal() + " items");
        return ResponseEntity.ok(items);
    }

    @GetMapping("/found")
    public ResponseEntity<?> getMyFoundItems(@AuthenticationPrincipal String username, 
                                             @RequestParam(defaultValue = "0") int page, 
                                             @RequestParam(defaultValue = "10") int size) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return ResponseEntity.ok(service.getUserFoundItems(user.getId(), page, size));
    }

    @GetMapping("/claims")
    public ResponseEntity<?> getMyClaims(@AuthenticationPrincipal String username, 
                                         @RequestParam(defaultValue = "0") int page, 
                                         @RequestParam(defaultValue = "10") int size) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Page<com.campus.lostfound.entity.FoundItem> items = service.getUserClaims(user.getId(), page, size);
        return ResponseEntity.ok(Map.of("lost", items.getRecords(), "total", items.getTotal()));
    }

    @PostMapping("/claims/{id}/unclaim")
    public ResponseEntity<?> unclaimItem(@AuthenticationPrincipal String username, @PathVariable Long id) {
        User user = userRepository.findByUsername(username).orElseThrow();
        service.unclaimFoundItem(id, user.getId());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/lost/{id}")
    public ResponseEntity<?> deleteLost(@AuthenticationPrincipal String username, @PathVariable Long id) {
        var user = userRepository.findByUsername(username).orElseThrow();
        service.deleteLostWithFiles(id);
        Message msg = new Message();
        msg.setUserId(user.getId());
        msg.setTitle("撤销发布成功");
        msg.setContent("已撤销失物发布并删除相关图片文件");
        msg.setType("AUDIT");
        msg.setIsRead(false);
        msg.setRelatedId(id);
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/found/{id}")
    public ResponseEntity<?> deleteFound(@AuthenticationPrincipal String username, @PathVariable Long id) {
        var user = userRepository.findByUsername(username).orElseThrow();
        service.deleteFoundWithFiles(id);
        Message msg = new Message();
        msg.setUserId(user.getId());
        msg.setTitle("撤销发布成功");
        msg.setContent("已撤销拾物发布并删除相关图片文件");
        msg.setType("AUDIT");
        msg.setIsRead(false);
        msg.setRelatedId(id);
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PutMapping("/lost/{id}/status")
    public ResponseEntity<?> updateLostStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        service.updateLostStatus(id, body.get("status"));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PutMapping("/found/{id}/status")
    public ResponseEntity<?> updateFoundStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        service.updateFoundStatus(id, body.get("status"));
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
