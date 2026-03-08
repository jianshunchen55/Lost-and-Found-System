package com.campus.lostfound.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.lostfound.entity.Notice;
import com.campus.lostfound.mapper.NoticeMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/notice")
public class NoticeController {
  private final NoticeMapper noticeMapper;
  public NoticeController(NoticeMapper noticeMapper) { this.noticeMapper = noticeMapper; }
  @GetMapping("/list")
  public ResponseEntity<?> list() { return ResponseEntity.ok(noticeMapper.selectList(new QueryWrapper<>())); }
  @PostMapping("/add")
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
  public ResponseEntity<?> add(@RequestBody Notice n) { n.setUpdatedAt(LocalDateTime.now()); noticeMapper.insert(n); return ResponseEntity.ok(n); }
  @PutMapping("/update")
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
  public ResponseEntity<?> update(@RequestBody Notice n) { n.setUpdatedAt(LocalDateTime.now()); noticeMapper.updateById(n); return ResponseEntity.ok(n); }
  @DeleteMapping("/delete")
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
  public ResponseEntity<?> delete(@RequestBody Map<String, Object> body) { noticeMapper.deleteById(Long.valueOf(body.get("id").toString())); return ResponseEntity.ok(Map.of("ok",true)); }
}
