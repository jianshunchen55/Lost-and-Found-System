package com.campus.lostfound.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.lostfound.entity.FoundItem;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.mapper.FoundItemMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/stat")
public class StatController {
  private final LostItemMapper lostItemMapper;
  private final FoundItemMapper foundItemMapper;

  public StatController(LostItemMapper lostItemMapper, FoundItemMapper foundItemMapper) { 
    this.lostItemMapper = lostItemMapper; 
    this.foundItemMapper = foundItemMapper;
  }

  @GetMapping("/home-summary")
  public ResponseEntity<?> homeSummary() {
    // This week lost count
    LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
    long lostWeekly = lostItemMapper.selectCount(new QueryWrapper<LostItem>()
        .ge("created_at", oneWeekAgo));

    // Total successful claims (Lost + Found)
    long claimedLost = lostItemMapper.selectCount(new QueryWrapper<LostItem>().eq("status", "CLAIMED"));
    long claimedFound = foundItemMapper.selectCount(new QueryWrapper<FoundItem>().eq("status", "CLAIMED"));
    
    return ResponseEntity.ok(Map.of(
        "lostWeekly", lostWeekly,
        "claimedWeekly", claimedLost + claimedFound
    ));
  }

    @GetMapping("/lostType")
    public ResponseEntity<?> lostType() {
        QueryWrapper<LostItem> qw = new QueryWrapper<>();
        qw.select("category", "count(*) as count").groupBy("category");
        java.util.List<Map<String, Object>> list = lostItemMapper.selectMaps(qw);
        
        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Map<String, Object> map : list) {
            String cat = (String) map.get("category");
            if (cat == null) cat = "其他";
            result.add(Map.of("name", getCategoryName(cat), "value", map.get("count")));
        }
        return ResponseEntity.ok(result);
    }

    private String getCategoryName(String key) {
        switch (key) {
            case "card": return "证件";
            case "electronic": return "电子产品";
            case "book": return "书籍";
            case "daily": return "生活用品";
            default: return "其他";
        }
    }

    @GetMapping("/trend")
    public ResponseEntity<?> trend() {
        java.time.LocalDate today = java.time.LocalDate.now();
        String[] dates = new String[7];
        long[] lostCounts = new long[7];
        long[] foundCounts = new long[7];
        long[] claimedCounts = new long[7];

        for (int i = 0; i < 7; i++) {
            java.time.LocalDate date = today.minusDays(6 - i);
            dates[i] = date.toString();
            
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            
            lostCounts[i] = lostItemMapper.selectCount(new QueryWrapper<LostItem>()
                .ge("created_at", start).lt("created_at", end));
                
            foundCounts[i] = foundItemMapper.selectCount(new QueryWrapper<FoundItem>()
                .ge("created_at", start).lt("created_at", end));
                
            claimedCounts[i] = lostItemMapper.selectCount(new QueryWrapper<LostItem>()
                .eq("status", "CLAIMED").ge("updated_at", start).lt("updated_at", end));
        }
        
        return ResponseEntity.ok(Map.of(
            "dates", dates,
            "lost", lostCounts,
            "found", foundCounts,
            "claimed", claimedCounts
        ));
    }

    @GetMapping("/hotLocation")
    public ResponseEntity<?> hotLocation() {
        QueryWrapper<LostItem> qw = new QueryWrapper<>();
        qw.select("location", "count(*) as count")
          .groupBy("location")
          .orderByDesc("count")
          .last("limit 10");
          
        java.util.List<Map<String, Object>> list = lostItemMapper.selectMaps(qw);
        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        
        for (Map<String, Object> map : list) {
            String loc = (String) map.get("location");
            if (loc == null || loc.trim().isEmpty() || loc.length() > 20) continue;
            result.add(Map.of("name", loc, "value", map.get("count")));
        }
        
        return ResponseEntity.ok(result);
    }
}
