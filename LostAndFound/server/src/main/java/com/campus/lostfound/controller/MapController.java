package com.campus.lostfound.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.lostfound.entity.MapPoint;
import com.campus.lostfound.mapper.MapPointMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/map")
public class MapController {
  private final MapPointMapper mapper;
  public MapController(MapPointMapper mapper) { this.mapper = mapper; }
  @GetMapping("/point")
  public ResponseEntity<?> list() { return ResponseEntity.ok(mapper.selectList(new QueryWrapper<>())); }
  @PostMapping("/point")
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
  public ResponseEntity<?> add(@RequestBody MapPoint p) { mapper.insert(p); return ResponseEntity.ok(p); }
  @PutMapping("/point")
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
  public ResponseEntity<?> update(@RequestBody MapPoint p) { mapper.updateById(p); return ResponseEntity.ok(p); }
  @DeleteMapping("/point")
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
  public ResponseEntity<?> delete(@RequestBody Map<String, Object> body) { mapper.deleteById(Long.valueOf(body.get("id").toString())); return ResponseEntity.ok(Map.of("ok",true)); }
}
