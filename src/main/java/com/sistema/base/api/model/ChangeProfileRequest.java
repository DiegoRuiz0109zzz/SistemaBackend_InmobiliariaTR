package com.sistema.base.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeProfileRequest {
    private Integer userId;
    private String newProfileName;
}