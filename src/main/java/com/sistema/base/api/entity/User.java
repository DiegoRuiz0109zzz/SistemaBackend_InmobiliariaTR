package com.sistema.base.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

	private static final long serialVersionUID = 1L;
	@Id
    @GeneratedValue
    private Integer id;
    @Column(unique = true, nullable = false)
    private String username;
    private String password;
    private String nombres;
    private String apellidos;
    private String email;
    private String telefono;
    @Column(unique = true, nullable = false)
    private String docIdentidad;
    
    @Column(name = "profile_image")
    private String profileImage;

    @ManyToOne
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<SimpleGrantedAuthority> authorities = profile.getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                .collect(Collectors.toList());
        
        // Agregar el nombre del perfil como un rol (con prefijo ROLE_)
        authorities.add(new SimpleGrantedAuthority("ROLE_" + profile.getName()));
        // También agregarlo directamente como autoridad
        authorities.add(new SimpleGrantedAuthority(profile.getName()));
        
        return authorities;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
