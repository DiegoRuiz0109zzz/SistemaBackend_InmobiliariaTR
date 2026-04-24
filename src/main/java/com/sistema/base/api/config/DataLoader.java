package com.sistema.base.api.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sistema.base.api.entity.Permission;
import com.sistema.base.api.entity.Profile;
import com.sistema.base.api.entity.Theme;
import com.sistema.base.api.entity.User;
import com.sistema.base.api.repository.PermissionRepository;
import com.sistema.base.api.repository.ProfileRepository;
import com.sistema.base.api.repository.ThemeRepository;
import com.sistema.base.api.repository.UserRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class DataLoader {

    private final PermissionRepository permissionRepository;
    private final ProfileRepository profileRepository;
    private final ThemeRepository themeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(PermissionRepository permissionRepository,
                      ProfileRepository profileRepository,
                      ThemeRepository themeRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.permissionRepository = permissionRepository;
        this.profileRepository = profileRepository;
        this.themeRepository = themeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {

            // 1. Definir y Crear todos los permisos del sistema (ACTUALIZADO CON FINANCIAMIENTO)
            List<String> nombresPermisos = Arrays.asList(
                    "ACCESO_BASICO", "GESTION_USUARIOS", "GESTION_ROLES", "GESTION_JERARQUIA",
                    "CREAR_EMPRESA", "EDITAR_EMPRESA", "ELIMINAR_EMPRESA",
                    "CREAR_CLIENTE", "EDITAR_CLIENTE", "ELIMINAR_CLIENTE",
                    "CREAR_INTERESADO", "EDITAR_INTERESADO", "ELIMINAR_INTERESADO",
                    "CREAR_URBANIZACION", "EDITAR_URBANIZACION", "ELIMINAR_URBANIZACION",
                    "CREAR_ETAPA", "EDITAR_ETAPA", "ELIMINAR_ETAPA",
                    "CREAR_MANZANA", "EDITAR_MANZANA", "ELIMINAR_MANZANA",
                    "CREAR_LOTE", "EDITAR_LOTE", "ELIMINAR_LOTE",
                    "CREAR_VENDEDOR", "EDITAR_VENDEDOR", "ELIMINAR_VENDEDOR",

                    // -- NUEVOS PERMISOS FINANCIEROS --
                    "CREAR_CONTRATO", "EDITAR_CONTRATO", "ELIMINAR_CONTRATO",
                    "CREAR_CUOTA", "EDITAR_CUOTA", "ELIMINAR_CUOTA",
                    "CREAR_PAGO", "EDITAR_PAGO", "ELIMINAR_PAGO",
                    "CREAR_PAGO","PROCESAR_PAGO","ELIMINAR_PAGO"
            );

            List<Permission> todosLosPermisos = new ArrayList<>();
            for (String nombre : nombresPermisos) {
                todosLosPermisos.add(createPermissionIfNotFound(nombre));
            }

            // Permiso básico individual para los roles menores
            Permission accesoBasico = todosLosPermisos.get(0);

            // 2. Crear Perfiles Corporativos (Terranort)

            // SUPER_ADMINISTRADOR - Máxima jerarquía con TODOS los permisos
            Profile superAdmin = profileRepository.findByName("SUPER_ADMINISTRADOR")
                    .orElseGet(() -> {
                        Profile p = new Profile();
                        p.setName("SUPER_ADMINISTRADOR");
                        p.setHierarchyLevel(100);
                        return p;
                    });
            // Limpiamos y reasignamos para asegurar que tenga los nuevos si se agregaron después
            superAdmin.getPermissions().clear();
            superAdmin.getPermissions().addAll(todosLosPermisos);
            profileRepository.save(superAdmin);

            // GERENTE GENERAL - Acceso total al negocio con TODOS los permisos
            Profile gerenteGeneral = profileRepository.findByName("GERENTE_GENERAL")
                    .orElseGet(() -> {
                        Profile p = new Profile();
                        p.setName("GERENTE_GENERAL");
                        p.setHierarchyLevel(95);
                        return p;
                    });
            gerenteGeneral.getPermissions().clear();
            gerenteGeneral.getPermissions().addAll(todosLosPermisos);
            profileRepository.save(gerenteGeneral);

            // Roles con permisos básicos iniciales
            createProfileIfNotFound("JEFE_ADMINISTRACION", 90, accesoBasico);
            createProfileIfNotFound("JEFE_VENTAS", 85, accesoBasico);
            createProfileIfNotFound("CONTADORA", 80, accesoBasico);
            createProfileIfNotFound("ABOGADA", 80, accesoBasico);
            createProfileIfNotFound("ADMINISTRADORA", 75, accesoBasico);
            createProfileIfNotFound("ASISTENTE_ADMINISTRATIVO", 70, accesoBasico);

            // 3. Crear Usuarios Iniciales

            // Usuario admin
            if (userRepository.findByUsername("admin").isEmpty()) {
                User adminUser = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin"))
                        .nombres("Administrador")
                        .apellidos("Terranort")
                        .email("admin@terranort.pe")
                        .telefono("999999999")
                        .docIdentidad("00000000")
                        .profile(superAdmin)
                        .build();
                userRepository.save(adminUser);
            }

            // Usuario diego (Gerente General)
            if (userRepository.findByUsername("diego").isEmpty()) {
                User diegoUser = User.builder()
                        .username("diego")
                        .password(passwordEncoder.encode("diego123"))
                        .nombres("Diego")
                        .apellidos("Ruiz")
                        .email("diego.ruiz@terranort.pe")
                        .telefono("987654321")
                        .docIdentidad("12345678")
                        .profile(gerenteGeneral)
                        .build();
                userRepository.save(diegoUser);
                System.out.println(">>> CONFIGURACIÓN INICIAL: Usuario 'diego' creado como GERENTE GENERAL");
            }

            // 4. Temas
            createThemeIfNotFound("base", "Clásico", "Tema azul profesional", "#357ABD", "#f4f7fa", "#357ABD", "#ffffff", "#ffffff", "#212529", "#6c757d", false, true);
            createThemeIfNotFound("dark", "Oscuro", "Modo oscuro nativo", "#5c6bc0", "#1a1a2e", "#16213e", "#e8e8e8", "#1f1f38", "#e8e8e8", "#a0a0a0", true, true);
        };
    }

    private void createProfileIfNotFound(String name, int level, Permission permission) {
        if (profileRepository.findByName(name).isEmpty()) {
            Profile profile = new Profile();
            profile.setName(name);
            profile.setHierarchyLevel(level);
            profile.getPermissions().add(permission);
            profileRepository.save(profile);
        }
    }

    private void createThemeIfNotFound(String key, String name, String desc, String primary, String bg, String topbar, String topbarText, String card, String textP, String textS, boolean isDark, boolean isSystem) {
        if (themeRepository.findByThemeKey(key).isEmpty()) {
            Theme theme = Theme.builder()
                    .themeKey(key)
                    .name(name)
                    .description(desc)
                    .primaryColor(primary)
                    .backgroundColor(bg)
                    .topbarColor(topbar)
                    .topbarTextColor(topbarText)
                    .cardBackground(card)
                    .textPrimary(textP)
                    .textSecondary(textS)
                    .isDark(isDark)
                    .isSystem(isSystem)
                    .build();
            themeRepository.save(theme);
        }
    }

    private Permission createPermissionIfNotFound(String name) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(new Permission(null, name)));
    }
}