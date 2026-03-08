package com.campus.lostfound.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.domain.User;
import com.campus.lostfound.entity.Message;
import com.campus.lostfound.mapper.MessageMapper;
import com.campus.lostfound.repo.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.campus.lostfound.service.ChatMessageService;

@RestController
@RequestMapping("/api/message")
public class UserMessageController {

    private final MessageMapper messageMapper;
    private final UserRepository userRepository;
    private final ChatMessageService chatMessageService;

    public UserMessageController(MessageMapper messageMapper, UserRepository userRepository, ChatMessageService chatMessageService) {
        this.messageMapper = messageMapper;
        this.userRepository = userRepository;
        this.chatMessageService = chatMessageService;
    }

    @GetMapping
    public ResponseEntity<?> getMyMessages(@AuthenticationPrincipal String username,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String type,
                                           @RequestParam(required = false) Boolean isRead) {
        User user = userRepository.findByUsername(username).orElseThrow();
        QueryWrapper<Message> qw = new QueryWrapper<>();
        qw.eq("user_id", user.getId());
        if (type != null && !type.isEmpty()) qw.eq("type", type);
        if (isRead != null) qw.eq("is_read", isRead);
        qw.orderByDesc("created_at");
        
        return ResponseEntity.ok(messageMapper.selectPage(new Page<>(page+1, size), qw));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@AuthenticationPrincipal String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Long count = messageMapper.selectCount(new QueryWrapper<Message>()
                .eq("user_id", user.getId())
                .eq("is_read", false));
        Long chatCount = chatMessageService.getUnreadCount(user.getId());
        return ResponseEntity.ok(Map.of("count", count + chatCount));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        Message msg = messageMapper.selectById(id);
        if (msg != null) {
            msg.setIsRead(true);
            messageMapper.updateById(msg);
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@AuthenticationPrincipal String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Message updateMsg = new Message();
        updateMsg.setIsRead(true);
        messageMapper.update(updateMsg, new QueryWrapper<Message>().eq("user_id", user.getId()).eq("is_read", false));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMessage(@PathVariable Long id) {
        messageMapper.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
