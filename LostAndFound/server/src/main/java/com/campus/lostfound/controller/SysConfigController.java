package com.campus.lostfound.controller;

import com.campus.lostfound.entity.SysConfig;
import com.campus.lostfound.mapper.SysConfigMapper;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class SysConfigController {
    private final SysConfigMapper mapper;
    public SysConfigController(SysConfigMapper mapper) { this.mapper = mapper; }
    
    @GetMapping("/{key}")
    public Object get(@PathVariable String key) {
        SysConfig c = mapper.selectById(key);
        return Map.of("value", c != null ? c.getCfgValue() : "");
    }
    
    @PostMapping
    public Object set(@RequestBody SysConfig config) {
        if (mapper.selectById(config.getCfgKey()) != null) {
            mapper.updateById(config);
        } else {
            mapper.insert(config);
        }
        return Map.of("ok", true);
    }
}
