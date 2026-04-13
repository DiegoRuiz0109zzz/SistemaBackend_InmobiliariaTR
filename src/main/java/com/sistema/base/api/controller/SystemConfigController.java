package com.sistema.base.api.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.sistema.base.api.entity.SystemConfig;
import com.sistema.base.api.entity.Theme;
import com.sistema.base.api.repository.SystemConfigRepository;
import com.sistema.base.api.repository.ThemeRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigRepository configRepository;
    private final ThemeRepository themeRepository;

    /**
     * Obtener el tema institucional actual (devuelve el objeto completo)
     */
    @GetMapping("/theme")
    public ResponseEntity<?> getCurrentTheme() {
        Optional<SystemConfig> config = configRepository.findByConfigKey("theme_id");
        String themeKey = config.map(SystemConfig::getConfigValue).orElse("base");
        
        Optional<Theme> theme = themeRepository.findByThemeKey(themeKey);
        if (theme.isPresent()) {
            return ResponseEntity.ok(theme.get());
        }
        
        // Si no existe el registro en themes, devolver un proxy de base
        return ResponseEntity.ok(Map.of("themeKey", "base", "name", "Clásico"));
    }

    /**
     * Listar todos los temas disponibles
     */
    @GetMapping("/themes")
    public ResponseEntity<List<Theme>> getAllThemes() {
        return ResponseEntity.ok(themeRepository.findAll());
    }

    /**
     * Establecer cuál es el tema institucional (solo SUPER_ADMINISTRADOR)
     */
    @PutMapping("/theme")
    @PreAuthorize("hasAuthority('SUPER_ADMINISTRADOR')")
    public ResponseEntity<?> setInstitutionalTheme(@RequestBody Map<String, String> request) {
        String themeKey = request.get("themeId");
        
        if (themeKey == null || themeKey.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El themeKey es requerido"));
        }

        Optional<Theme> theme = themeRepository.findByThemeKey(themeKey);
        if (theme.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El tema no existe en la base de datos"));
        }

        Optional<SystemConfig> existingConfig = configRepository.findByConfigKey("theme_id");
        SystemConfig config = existingConfig.orElse(new SystemConfig(null, "theme_id", "base"));
        config.setConfigValue(themeKey);
        configRepository.save(config);
        
        return ResponseEntity.ok(Map.of(
            "message", "Tema institucional actualizado",
            "theme", theme.get()
        ));
    }

    /**
     * Crear o actualizar un tema (solo SUPER_ADMINISTRADOR)
     */
    @PostMapping("/themes")
    @PreAuthorize("hasAuthority('SUPER_ADMINISTRADOR')")
    public ResponseEntity<?> saveTheme(@RequestBody Theme theme) {
        if (theme.getThemeKey() == null || theme.getThemeKey().isEmpty()) {
            // Generar un key amigable si no viene
            theme.setThemeKey(theme.getName().toLowerCase().replaceAll("\\s+", "_"));
        }
        
        // No permitir sobrescribir temas de sistema manualmente por este endpoint sin cuidado?
        // O simplemente permitirlo. Procedo a guardar.
        Theme saved = themeRepository.save(theme);
        return ResponseEntity.ok(saved);
    }

    /**
     * Eliminar un tema (solo SUPER_ADMINISTRADOR)
     */
    @DeleteMapping("/themes/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMINISTRADOR')")
    public ResponseEntity<?> deleteTheme(@PathVariable Long id) {
        Optional<Theme> theme = themeRepository.findById(id);
        if (theme.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        if (theme.get().isSystem()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pueden eliminar temas del sistema"));
        }
        
        // Verificar si es el tema actual
        Optional<SystemConfig> config = configRepository.findByConfigKey("theme_id");
        if (config.isPresent() && config.get().getConfigValue().equals(theme.get().getThemeKey())) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se puede eliminar el tema que está en uso actualmente"));
        }

        themeRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Tema eliminado correctamente"));
    }
}
