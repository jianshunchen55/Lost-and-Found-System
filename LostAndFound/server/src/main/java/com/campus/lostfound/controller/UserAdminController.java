package com.campus.lostfound.controller;

import com.campus.lostfound.domain.Role;
import com.campus.lostfound.domain.User;
import com.campus.lostfound.repo.RoleRepository;
import com.campus.lostfound.repo.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class UserAdminController {
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  public UserAdminController(UserRepository userRepository, RoleRepository roleRepository) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
  }
  @GetMapping("/list")
  public ResponseEntity<?> list() { return ResponseEntity.ok(userRepository.findAll()); }
  @PostMapping("/add")
  public ResponseEntity<?> add(@RequestBody User u) { return ResponseEntity.ok(userRepository.save(u)); }
  @PutMapping("/update")
  public ResponseEntity<?> update(@RequestBody Map<String, Object> body) {
    Long id = Long.valueOf(body.get("id").toString());
    String roleCode = body.get("role").toString();
    User u = userRepository.findById(id).orElse(null);
    Role r = roleRepository.findByCode(roleCode).orElse(null);
    if (u==null || r==null) return ResponseEntity.badRequest().body(Map.of("message","参数错误"));
    u.setRoles(new java.util.HashSet<>(java.util.Set.of(r)));
    return ResponseEntity.ok(userRepository.save(u));
  }
  @DeleteMapping("/delete")
  public ResponseEntity<?> delete(@RequestBody Map<String, Object> body) {
    Long id = Long.valueOf(body.get("id").toString());
    userRepository.deleteById(id);
    return ResponseEntity.ok(Map.of("ok", true));
  }
}
