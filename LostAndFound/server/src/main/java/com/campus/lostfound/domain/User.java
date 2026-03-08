package com.campus.lostfound.domain;

import lombok.Data;
import javax.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    private boolean enabled = true;

    private boolean audited = false;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    private String nickname;
    private String department;
    private String phone;
    private String avatar;
    private String bio;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
