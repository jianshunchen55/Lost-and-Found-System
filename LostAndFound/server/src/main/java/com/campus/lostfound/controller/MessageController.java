package com.campus.lostfound.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.lostfound.entity.FoundItem;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.mapper.FoundItemMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.util.MatchUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class MessageController {
  private final LostItemMapper lostItemMapper;
  private final FoundItemMapper foundItemMapper;
  public MessageController(LostItemMapper lostItemMapper, FoundItemMapper foundItemMapper) {
    this.lostItemMapper = lostItemMapper;
    this.foundItemMapper = foundItemMapper;
  }
  @PostMapping("/message/push")
  public ResponseEntity<?> push(@RequestBody Map<String, Object> body) {
    Long itemId = Long.valueOf(body.get("itemId").toString());
    LostItem lost = lostItemMapper.selectById(itemId);
    if (lost == null) return ResponseEntity.badRequest().body(Map.of("message","数据不存在"));
    List<FoundItem> candidates = foundItemMapper.selectList(new QueryWrapper<>());
    double bestScore = 0;
    FoundItem best = null;
    for (FoundItem f : candidates) {
      double s = MatchUtil.score(lost.getCategory(), f.getCategory(),
          lost.getCreatedAt()==null?0:lost.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
          f.getCreatedAt()==null?0:f.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
          lost.getLatitude()==null?0:lost.getLatitude(),
          lost.getLongitude()==null?0:lost.getLongitude(),
          f.getLatitude()==null?0:f.getLatitude(),
          f.getLongitude()==null?0:f.getLongitude(),
          lost.getDescription(), f.getDescription());
      if (s > bestScore) { bestScore = s; best = f; }
    }
    Map<String,Object> result = new HashMap<>();
    result.put("score", Math.round(bestScore*100));
    result.put("candidate", best);
    return ResponseEntity.ok(result);
  }
  @GetMapping("/message/list")
  public ResponseEntity<?> list(@RequestParam String status) {
    return ResponseEntity.ok(List.of());
  }
  @PutMapping("/message/read")
  public ResponseEntity<?> read(@RequestBody Map<String, Object> body) {
    return ResponseEntity.ok(Map.of("ok", true));
  }
}
