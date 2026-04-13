package com.sistema.base.api.controller;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.sistema.base.api.entity.Permission;
import com.sistema.base.api.service.PermissionService;
import com.sistema.base.api.service.ProfileService;

@RestController
@RequestMapping("/api/admin/profiles")
public class ProfileManagementController {

    private final ProfileService profileService;
    private final PermissionService permissionService;

    public ProfileManagementController(ProfileService profileService, PermissionService permissionService) {
        this.profileService = profileService;
        this.permissionService = permissionService;
    }

    @PostMapping("/{profileId}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('GESTION_ROLES')")
    public ResponseEntity<String> addPermission(@PathVariable Long profileId, @PathVariable Long permissionId) {
        try {
            profileService.addPermissionToProfile(profileId, permissionId);
            return ResponseEntity.ok("Permiso agregado exitosamente.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{profileId}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('GESTION_ROLES')")
    public ResponseEntity<String> removePermission(@PathVariable Long profileId, @PathVariable Long permissionId) {
        try {
            profileService.removePermissionFromProfile(profileId, permissionId);
            return ResponseEntity.ok("Permiso removido exitosamente.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('GESTION_ROLES')")
    public ResponseEntity<Set<Permission>> getAllPermissions() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        com.sistema.base.api.entity.User user = (com.sistema.base.api.entity.User) authentication.getPrincipal();
        Set<Permission> permissions = permissionService.getPermissionsForRole(user.getProfile().getName());
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<com.sistema.base.api.entity.Profile>> getAllProfiles() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        com.sistema.base.api.entity.User user = (com.sistema.base.api.entity.User) authentication.getPrincipal();
        return ResponseEntity.ok(profileService.getProfilesForRole(user.getProfile().getName()));
    }
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('GESTION_ROLES')")
    public ResponseEntity<?> createProfile(@RequestBody java.util.Map<String, String> body) {
        try {
            return ResponseEntity.ok(profileService.createProfile(body.get("name")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GESTION_ROLES')")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        try {
            return ResponseEntity.ok(profileService.updateProfileName(id, body.get("name")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('GESTION_ROLES')")
    public ResponseEntity<?> deleteProfile(@PathVariable Long id) {
        try {
            profileService.deleteProfile(id);
            return ResponseEntity.ok("Perfil eliminado.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('GESTION_JERARQUIA')")
    public ResponseEntity<?> reorderProfiles(@RequestBody java.util.List<Long> orderedIds) {
        try {
            profileService.updateHierarchy(orderedIds);
            return ResponseEntity.ok("Jerarquía actualizada.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}