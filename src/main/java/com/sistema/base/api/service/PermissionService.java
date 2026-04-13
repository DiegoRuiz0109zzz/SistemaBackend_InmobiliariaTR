package com.sistema.base.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sistema.base.api.entity.Permission;
import com.sistema.base.api.repository.PermissionRepository;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public Set<Permission> findAll() {
        return new HashSet<>(permissionRepository.findAll());
    }

    public Permission findById(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permiso no encontrado con ID: " + id));
    }

    public Set<Permission> getPermissionsForRole(String roleName) {
        // All roles can see all available permissions
        // The actual assignment is controlled by the UI and role hierarchy
        return new HashSet<>(permissionRepository.findAll());
    }
}