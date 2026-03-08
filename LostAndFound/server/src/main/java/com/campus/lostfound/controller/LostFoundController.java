package com.campus.lostfound.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.entity.FoundItem;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.service.LostFoundService;
import com.campus.lostfound.dto.LostItemDTO;
import com.campus.lostfound.dto.FoundItemDTO;
import com.campus.lostfound.entity.LostItem;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.campus.lostfound.repo.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api")
public class LostFoundController {
  private final LostFoundService service;
  private final UserRepository userRepository;

  public LostFoundController(LostFoundService service, UserRepository userRepository) { 
    this.service = service;
    this.userRepository = userRepository;
  }

  @PostMapping("/lost/add")
  public ResponseEntity<?> addLost(@AuthenticationPrincipal String username, @Validated @RequestBody LostItemDTO body) {
    System.out.println("DEBUG: addLost called by " + username);
    return userRepository.findByUsername(username).map(user -> {
        System.out.println("DEBUG: User found, ID: " + user.getId());
        LostItem item = new LostItem();
        item.setTitle(body.getTitle());
        item.setCategory(body.getCategory());
        item.setDescription(body.getDescription());
        item.setLocation(body.getLocation());
        item.setLatitude(body.getLatitude());
        item.setLongitude(body.getLongitude());
        item.setImageUrl(body.getImageUrl());
        item.setThumbnailUrl(body.getThumbnailUrl());
        item.setPointId(body.getPointId());
        item.setContact(body.getContact());
        item.setImages(body.getImages());
        if (body.getLostTime() != null && !body.getLostTime().isEmpty()) {
          try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            item.setLostTime(LocalDateTime.parse(body.getLostTime(), fmt));
          } catch (Exception e) {
              // Try ISO format as fallback
              try {
                  item.setLostTime(LocalDateTime.parse(body.getLostTime()));
              } catch (Exception ignored) {}
          }
        }
        item.setUserId(user.getId());
        item.setAuditStatus(0);
        item.setClaimStatus(0);
        service.addLost(item);
        return ResponseEntity.ok(java.util.Map.of("id", String.valueOf(item.getId()), "message", "发布成功"));
    }).orElse(ResponseEntity.status(401).build());
  }

    @PostMapping("/found/add")
  public ResponseEntity<?> addFound(@AuthenticationPrincipal String username, @Validated @RequestBody FoundItemDTO body) {
    try {
        return userRepository.findByUsername(username).map(user -> {
            FoundItem item = new FoundItem();
            item.setTitle(body.getTitle());
            item.setCategory(body.getCategory());
            item.setDescription(body.getDescription());
            item.setLocation(body.getLocation());
            item.setLatitude(body.getLatitude());
            item.setLongitude(body.getLongitude());
            item.setImageUrl(body.getImageUrl());
            item.setThumbnailUrl(body.getThumbnailUrl());
            item.setPointId(body.getPointId());
            item.setContact(body.getContact());
            item.setImages(body.getImages());
            if (body.getFoundTime() != null && !body.getFoundTime().isEmpty()) {
              try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                item.setFoundTime(LocalDateTime.parse(body.getFoundTime(), fmt));
              } catch (Exception e) {
                  // Try ISO format as fallback
                  try {
                      item.setFoundTime(LocalDateTime.parse(body.getFoundTime()));
                  } catch (Exception ignored) {}
              }
            }
            item.setUserId(user.getId());
            item.setAuditStatus(0);
            item.setClaimStatus(0);
            service.addFound(item);
            return ResponseEntity.ok(java.util.Map.of("id", String.valueOf(item.getId()), "message", "发布成功"));
        }).orElse(ResponseEntity.status(401).build());
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(500).body(java.util.Map.of("error", e.getMessage()));
    }
  }

  @GetMapping("/smart-match")
  public ResponseEntity<?> getSmartMatches(@AuthenticationPrincipal String username) {
      if (username == null) return ResponseEntity.ok(java.util.Collections.emptyList());
      return userRepository.findByUsername(username)
          .map(user -> ResponseEntity.ok(service.getSmartMatches(user.getId())))
          .orElse(ResponseEntity.ok(java.util.Collections.emptyList()));
  }

  @GetMapping("/lost/query")
  public ResponseEntity<?> queryLost(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(required = false) String category, @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String location, @RequestParam(required = false) String status) {
    Page<LostItem> p = service.queryLost(page, size, category, keyword, location, status);
    return ResponseEntity.ok(p);
  }

  @GetMapping("/admin/lost/list")
    public ResponseEntity<?> adminListLost(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) Integer auditStatus) {
        return ResponseEntity.ok(service.queryLostByAuditStatus(page, size, auditStatus));
    }

    @GetMapping("/admin/found/list")
    public ResponseEntity<?> adminListFound(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) Integer auditStatus) {
        return ResponseEntity.ok(service.queryFoundByAuditStatus(page, size, auditStatus));
    }

  @PutMapping("/lost/audit")
  public ResponseEntity<?> auditLost(@RequestBody java.util.Map<String, Object> body) {
      Long id = Long.valueOf(body.get("id").toString());
      Integer status = Integer.valueOf(body.get("auditStatus").toString());
      LostItem item = service.getLostById(id);
      if (item != null) {
          item.setAuditStatus(status);
          if (status == 1) item.setStatus("APPROVED");
          else if (status == 2) item.setStatus("RETURNED");
          else if (status == 0) item.setStatus("PENDING");
          service.updateLost(item);
          return ResponseEntity.ok().build();
      }
      return ResponseEntity.notFound().build();
  }

  @PutMapping("/found/audit")
  public ResponseEntity<?> auditFound(@RequestBody java.util.Map<String, Object> body) {
      Long id = Long.valueOf(body.get("id").toString());
      Integer status = Integer.valueOf(body.get("auditStatus").toString());
      FoundItem item = service.getFoundById(id);
      if (item != null) {
          item.setAuditStatus(status);
          if (status == 1) item.setStatus("APPROVED");
          else if (status == 2) item.setStatus("RETURNED");
          else if (status == 0) item.setStatus("PENDING");
          service.updateFound(item);
          return ResponseEntity.ok().build();
      }
      return ResponseEntity.notFound().build();
  }

  @PostMapping("/lost/claim")
  public ResponseEntity<?> applyLostClaim(@AuthenticationPrincipal String username, @RequestBody java.util.Map<String, Object> body) {
      return userRepository.findByUsername(username).map(user -> {
          Long id = Long.valueOf(body.get("id").toString());
          LostItem item = service.getLostById(id);
          // Allow re-claim if status is 0 (new) or -1 (rejected)
          if (item != null && item.getAuditStatus() == 1 && (item.getClaimStatus() == 0 || item.getClaimStatus() == -1)) {
              item.setClaimUserId(user.getId());
              item.setClaimTime(LocalDateTime.now());
              item.setClaimStatus(1); // Pending Claim Audit
              service.updateLost(item);
              return ResponseEntity.ok().build();
          }
          return ResponseEntity.badRequest().body("无法申请认领");
      }).orElse(ResponseEntity.status(401).build());
  }

  @PostMapping("/found/claim")
  public ResponseEntity<?> applyFoundClaim(@AuthenticationPrincipal String username, @RequestBody java.util.Map<String, Object> body) {
      return userRepository.findByUsername(username).map(user -> {
          Long id = Long.valueOf(body.get("id").toString());
          String name = (String) body.get("name");
          String phone = (String) body.get("phone");
          
          if (name == null || name.isEmpty() || phone == null || phone.isEmpty()) {
              return ResponseEntity.badRequest().body("请填写姓名和电话");
          }

          FoundItem item = service.getFoundById(id);
          // Allow re-claim if status is 0 (new) or -1 (rejected)
          if (item != null && item.getAuditStatus() == 1 && (item.getClaimStatus() == 0 || item.getClaimStatus() == -1)) {
              item.setClaimUserId(user.getId());
              item.setClaimTime(LocalDateTime.now());
              item.setClaimName(name);
              item.setClaimPhone(phone);
              item.setClaimStatus(1); // Pending Claim Audit
              item.setClaimAuditReply(null); // Clear previous audit reply
              service.updateFound(item);
              return ResponseEntity.ok().build();
          }
          return ResponseEntity.badRequest().body("无法申请认领");
      }).orElse(ResponseEntity.status(401).build());
  }
  
  @PutMapping("/lost/claim-audit")
  public ResponseEntity<?> auditLostClaim(@RequestBody java.util.Map<String, Object> body) {
      Long id = Long.valueOf(body.get("id").toString());
      Integer status = Integer.valueOf(body.get("claimStatus").toString());
      String reply = (String) body.get("reply");
      LostItem item = service.getLostById(id);
      if (item != null) {
          service.auditLostClaim(id, status, reply);
          return ResponseEntity.ok().build();
      }
      return ResponseEntity.notFound().build();
  }

  @PutMapping("/found/claim-audit")
  public ResponseEntity<?> auditFoundClaim(@RequestBody java.util.Map<String, Object> body) {
      Long id = Long.valueOf(body.get("id").toString());
      Integer status = Integer.valueOf(body.get("claimStatus").toString());
      String reply = (String) body.get("reply");
      FoundItem item = service.getFoundById(id);
      if (item != null) {
          service.auditFoundClaim(id, status, reply);
          return ResponseEntity.ok().build();
      }
      return ResponseEntity.notFound().build();
  }
  
  @GetMapping("/admin/lost/claims")
  public ResponseEntity<?> adminListLostClaims(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) Integer claimStatus) {
      return ResponseEntity.ok(service.queryLostClaims(page, size, claimStatus));
  }

  @GetMapping("/admin/found/claims")
  public ResponseEntity<?> adminListFoundClaims(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) Integer claimStatus) {
      return ResponseEntity.ok(service.queryFoundClaims(page, size, claimStatus));
  }

  @GetMapping("/lost/{id}")
  public ResponseEntity<?> getLost(@PathVariable Long id) {
      LostItem item = service.getLostById(id);
      return item != null ? ResponseEntity.ok(item) : ResponseEntity.notFound().build();
  }
  
  @GetMapping("/found/{id}")
  public ResponseEntity<?> getFound(@PathVariable Long id) {
      FoundItem item = service.getFoundById(id);
      return item != null ? ResponseEntity.ok(item) : ResponseEntity.notFound().build();
  }

  @PutMapping("/lost/update")
  public ResponseEntity<?> updateLost(@AuthenticationPrincipal String username, @Validated @RequestBody LostItemDTO body) {
      return userRepository.findByUsername(username).map(user -> {
          // Verify ownership
          LostItem existing = service.getLostById(body.getId());
          if (existing == null) return ResponseEntity.notFound().build();
          boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getCode().contains("ADMIN"));
          if (!existing.getUserId().equals(user.getId()) && !isAdmin) {
              return ResponseEntity.status(403).build();
          }
          
          existing.setTitle(body.getTitle());
          existing.setCategory(body.getCategory());
          existing.setDescription(body.getDescription());
          existing.setLocation(body.getLocation());
          existing.setLatitude(body.getLatitude());
          existing.setLongitude(body.getLongitude());
          existing.setImageUrl(body.getImageUrl());
          existing.setThumbnailUrl(body.getThumbnailUrl());
          existing.setPointId(body.getPointId());
          existing.setContact(body.getContact());
          existing.setImages(body.getImages());
          
          if (body.getLostTime() != null && !body.getLostTime().isEmpty()) {
            try {
              DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
              existing.setLostTime(LocalDateTime.parse(body.getLostTime(), fmt));
            } catch (Exception ignored) {}
          } else {
              existing.setLostTime(null);
          }
          
          existing.setAuditStatus(0); // Reset to pending audit on update
          existing.setStatus("PENDING");
          existing.setClaimStatus(0); // Reset claim status
          existing.setClaimUserId(null);
          existing.setClaimTime(null);
          service.updateLost(existing);
          return ResponseEntity.ok().build();
      }).orElse(ResponseEntity.status(401).build());
  }

  @PutMapping("/found/update")
  public ResponseEntity<?> updateFound(@AuthenticationPrincipal String username, @Validated @RequestBody FoundItemDTO body) {
      return userRepository.findByUsername(username).map(user -> {
          FoundItem existing = service.getFoundById(body.getId());
          if (existing == null) return ResponseEntity.notFound().build();
          boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getCode().contains("ADMIN"));
          if (!existing.getUserId().equals(user.getId()) && !isAdmin) {
              return ResponseEntity.status(403).build();
          }
          
          existing.setTitle(body.getTitle());
          existing.setCategory(body.getCategory());
          existing.setDescription(body.getDescription());
          existing.setLocation(body.getLocation());
          existing.setLatitude(body.getLatitude());
          existing.setLongitude(body.getLongitude());
          existing.setImageUrl(body.getImageUrl());
          existing.setThumbnailUrl(body.getThumbnailUrl());
          existing.setPointId(body.getPointId());
          existing.setContact(body.getContact());
          existing.setImages(body.getImages());
          
          if (body.getFoundTime() != null && !body.getFoundTime().isEmpty()) {
            try {
              DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
              existing.setFoundTime(LocalDateTime.parse(body.getFoundTime(), fmt));
            } catch (Exception ignored) {}
          }
          
          existing.setAuditStatus(0);
          existing.setStatus("PENDING");
          existing.setClaimStatus(0); // Reset claim status
          existing.setClaimUserId(null);
          existing.setClaimTime(null);
          service.updateFound(existing);
          return ResponseEntity.ok().build();
      }).orElse(ResponseEntity.status(401).build());
  }

  @GetMapping("/found/query")
  public ResponseEntity<?> queryFound(@AuthenticationPrincipal String username,
                                     @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(required = false) String category, @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String location, @RequestParam(required = false) String status) {
    Long userId = null;
    if (username != null) {
        var user = userRepository.findByUsername(username).orElse(null);
        if (user != null) userId = user.getId();
    }
    Page<FoundItem> p = service.queryFound(page, size, category, keyword, location, status, null, userId);
    return ResponseEntity.ok(p);
  }

  @DeleteMapping("/admin/lost/{id}")
  public ResponseEntity<?> deleteLostAdmin(@PathVariable Long id) {
      service.deleteLostWithFiles(id);
      return ResponseEntity.ok().build();
  }

  @DeleteMapping("/admin/found/{id}")
  public ResponseEntity<?> deleteFoundAdmin(@PathVariable Long id) {
      service.deleteFoundWithFiles(id);
      return ResponseEntity.ok().build();
  }

  @DeleteMapping("/admin/lost/claims/{id}")
  public ResponseEntity<?> deleteLostClaim(@PathVariable Long id) {
      service.deleteLostClaim(id);
      return ResponseEntity.ok().build();
  }

  @DeleteMapping("/admin/found/claims/{id}")
  public ResponseEntity<?> deleteFoundClaim(@PathVariable Long id) {
      service.deleteFoundClaim(id);
      return ResponseEntity.ok().build();
  }
}
