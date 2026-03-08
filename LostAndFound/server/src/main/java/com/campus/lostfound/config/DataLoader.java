package com.campus.lostfound.config;

import com.campus.lostfound.domain.Role;
import com.campus.lostfound.domain.User;
import com.campus.lostfound.repo.RoleRepository;
import com.campus.lostfound.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.util.Set;

@Configuration
public class DataLoader {
  @Bean
  public CommandLineRunner init(RoleRepository roleRepo, UserRepository userRepo, PasswordEncoder encoder) {
    return args -> {
      try {
          Role student = roleRepo.findByCode("ROLE_STUDENT").orElseGet(() -> {
               Role r = new Role();
               r.setCode("ROLE_STUDENT");
               r.setName("学生");
               return roleRepo.save(r);
           });
           Role admin = roleRepo.findByCode("ROLE_ADMIN").orElseGet(() -> {
               Role r = new Role();
               r.setCode("ROLE_ADMIN");
               r.setName("管理员");
               return roleRepo.save(r);
           });
           Role manager = roleRepo.findByCode("ROLE_MANAGER").orElseGet(() -> {
               Role r = new Role();
               r.setCode("ROLE_MANAGER");
               r.setName("负责人");
               return roleRepo.save(r);
           });
          if (userRepo.findByUsername("stu001").isEmpty()) {
            User u = new User();
            u.setUsername("stu001");
            u.setEmail("stu001@example.com");
            u.setPasswordHash(encoder.encode("12345678"));
            u.setEnabled(true);
            u.setAudited(true);
            u.setCreatedAt(Instant.now());
            u.setRoles(Set.of(student));
            userRepo.save(u);
          }
          if (userRepo.findByUsername("admin001").isEmpty()) {
            User u = new User();
            u.setUsername("admin001");
            u.setEmail("admin001@example.com");
            u.setPasswordHash(encoder.encode("12345678"));
            u.setEnabled(true);
            u.setAudited(true);
            u.setCreatedAt(Instant.now());
            u.setRoles(Set.of(admin));
            userRepo.save(u);
          }
          if (userRepo.findByUsername("mgr001").isEmpty()) {
            User u = new User();
            u.setUsername("mgr001");
            u.setEmail("mgr001@example.com");
            u.setPasswordHash(encoder.encode("12345678"));
            u.setEnabled(true);
            u.setAudited(true);
            u.setCreatedAt(Instant.now());
            u.setRoles(Set.of(manager));
            userRepo.save(u);
          }
      } catch (Exception e) {
          e.printStackTrace();
          throw e;
      }
    };
  }
}
