package com.campus.lostfound.controller;

import com.campus.lostfound.repo.UserRepository;
import com.campus.lostfound.service.FriendshipService;
import com.campus.lostfound.dto.FriendDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friend")
public class FriendshipController {
    private final FriendshipService service;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public FriendshipController(FriendshipService service, UserRepository userRepository, JdbcTemplate jdbcTemplate) {
        this.service = service;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addFriend(@AuthenticationPrincipal String username, @RequestBody Map<String, Long> body) {
        return userRepository.findByUsername(username).map(user -> {
            Long targetId = body.get("targetId");
            boolean success = service.sendRequest(user.getId(), targetId);
            if (!success) {
                return ResponseEntity.status(409).body("ALREADY_FRIENDS");
            }
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.status(401).build());
    }

    @PostMapping("/accept")
    public ResponseEntity<?> acceptRequest(@AuthenticationPrincipal String username, @RequestBody Map<String, Object> body) {
        try {
            return userRepository.findByUsername(username).map(user -> {
                try {
                    Object fidObj = body.get("friendshipId");
                    if (fidObj == null) {
                        System.err.println("FriendshipId is null");
                        return ResponseEntity.badRequest().body("FriendshipId is missing");
                    }
                    Long friendshipId = Long.valueOf(fidObj.toString());
                    String remark = (String) body.get("remark");
                    System.out.println("Accepting friend request: " + friendshipId + " by " + user.getUsername() + " with remark: " + remark);
                    
                    service.respondRequest(friendshipId, user.getId(), true, remark);
                    return ResponseEntity.ok().build();
                } catch (Exception e) {
                    if (e.getMessage() != null && (e.getMessage().contains("Unknown column") || e.getMessage().contains("not found in"))) {
                        System.out.println("Attempting to auto-fix schema...");
                        try {
                            jdbcTemplate.execute("ALTER TABLE friendship ADD COLUMN requester_alias VARCHAR(64)");
                        } catch (Exception ex) {}
                        try {
                            jdbcTemplate.execute("ALTER TABLE friendship ADD COLUMN addressee_alias VARCHAR(64)");
                        } catch (Exception ex) {}
                        
                        // Retry
                        try {
                            service.respondRequest(Long.valueOf(body.get("friendshipId").toString()), user.getId(), true, (String)body.get("remark"));
                            return ResponseEntity.ok().build();
                        } catch (Exception ex) {
                             ex.printStackTrace();
                             return ResponseEntity.status(500).body("Auto-fix failed: " + ex.getMessage());
                        }
                    }
                    e.printStackTrace();
                    return ResponseEntity.status(500).body(e.getMessage());
                }
            }).orElse(ResponseEntity.status(401).build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/reject")
    public ResponseEntity<?> rejectRequest(@AuthenticationPrincipal String username, @RequestBody Map<String, Object> body) {
        return userRepository.findByUsername(username).map(user -> {
            Long friendshipId = Long.valueOf(body.get("friendshipId").toString());
            service.respondRequest(friendshipId, user.getId(), false, null);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.status(401).build());
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@AuthenticationPrincipal String username, @RequestParam Long targetId) {
        return userRepository.findByUsername(username).map(user -> {
            String status = service.getStatus(user.getId(), targetId);
            return ResponseEntity.ok(Map.of("status", status));
        }).orElse(ResponseEntity.status(401).build());
    }

    @PostMapping("/alias")
    public ResponseEntity<?> updateAlias(@AuthenticationPrincipal String username, @RequestBody Map<String, Object> body) {
        return userRepository.findByUsername(username).map(user -> {
            Long targetId = Long.valueOf(body.get("targetId").toString());
            String alias = (String) body.get("alias");
            boolean success = service.updateAlias(user.getId(), targetId, alias);
            if (success) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest().body("Friendship not found or not accepted");
            }
        }).orElse(ResponseEntity.status(401).build());
    }

    @GetMapping("/list")
    public ResponseEntity<List<FriendDto>> getFriends(@AuthenticationPrincipal String username) {
        return userRepository.findByUsername(username).map(user -> {
            return ResponseEntity.ok(service.getFriends(user.getId()));
        }).orElse(ResponseEntity.status(401).build());
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<?> deleteFriend(@AuthenticationPrincipal String username, @PathVariable Long friendId) {
        return userRepository.findByUsername(username).map(user -> {
            boolean success = service.deleteFriend(user.getId(), friendId);
            if (success) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest().body("Friendship not found");
            }
        }).orElse(ResponseEntity.status(401).build());
    }
}
