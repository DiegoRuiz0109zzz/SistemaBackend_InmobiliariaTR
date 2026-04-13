package com.sistema.base.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "themes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Theme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String themeKey;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "background_color")
    private String backgroundColor;

    @Column(name = "topbar_color")
    private String topbarColor;

    @Column(name = "topbar_text_color")
    private String topbarTextColor;

    @Column(name = "card_background")
    private String cardBackground;

    @Column(name = "text_primary")
    private String textPrimary;

    @Column(name = "text_secondary")
    private String textSecondary;

    @Builder.Default
    private boolean isDark = false;

    @Builder.Default
    private boolean isSystem = false;
}
