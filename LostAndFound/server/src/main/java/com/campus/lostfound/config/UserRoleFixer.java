package com.campus.lostfound.config;

import com.campus.lostfound.domain.Role;
import com.campus.lostfound.domain.User;
import com.campus.lostfound.repo.RoleRepository;
import com.campus.lostfound.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class UserRoleFixer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserRoleFixer(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Ensure roles exist
        Role studentRole = createRoleIfNotFound("ROLE_STUDENT", "学生");
        Role managerRole = createRoleIfNotFound("ROLE_MANAGER", "负责人");
        Role adminRole = createRoleIfNotFound("ROLE_ADMIN", "管理员");

        // Fix mgr001 and mgr002 specifically
        fixUserRole("mgr001", managerRole);
        fixUserRole("mgr002", managerRole);

        // Find all users
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (user.getRoles().isEmpty()) {
                System.out.println("Fixing user with no roles: " + user.getUsername());
                user.getRoles().add(studentRole);
                userRepository.save(user);
            }
        }
    }

    private Role createRoleIfNotFound(String code, String name) {
        return roleRepository.findByCode(code)
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setCode(code);
                    r.setName(name);
                    return roleRepository.save(r);
                });
    }

    private void fixUserRole(String username, Role role) {
        userRepository.findByUsername(username).ifPresent(user -> {
            boolean hasRole = user.getRoles().stream().anyMatch(r -> r.getCode().equals(role.getCode()));
            if (!hasRole) {
                System.out.println("Fixing role for " + username);
                // Clear existing roles if you want to enforce ONLY this role, 
                // or just add it. Given the requirement "fix mgr001/002", usually implies setting them to Manager.
                // Managers shouldn't necessarily be students too, so let's clear and set.
                user.getRoles().clear();
                user.getRoles().add(role);
                userRepository.save(user);
            }
        });
    }
}
