package com.sistema.base.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "profiles")
@Getter
@Setter
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    /**
     * Nivel jerárquico del rol. Mayor número = Mayor jerarquía.
     * Ejemplo: SUPER_ADMIN=100, ADMIN=50, USER=1.
     * Esto permite una gestión dinámica sin hardcode.
     */
    @Column(name = "hierarchy_level")
    private Integer hierarchyLevel = 0;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "profile_permissions",
        joinColumns = @JoinColumn(name = "profile_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
}
