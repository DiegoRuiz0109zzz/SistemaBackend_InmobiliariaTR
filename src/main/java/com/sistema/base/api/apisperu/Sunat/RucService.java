package com.sistema.base.api.apisperu.Sunat;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RucService {

    private final RestTemplate restTemplate;

    @Value("${apisperu.token:}")
    private String apiPeruToken;

    private static final String PRIMARY_API_URL = "https://api.apis.net.pe/v1/ruc";
    private static final String BACKUP_API_URL = "https://dniruc.apisperu.com/api/v1";

    private static final Pattern RUC_PATTERN = Pattern.compile("^\\d{11}$");

    public Map<String, Object> consultarRUC(String ruc) {
        if (ruc == null || ruc.trim().isEmpty()) {
            throw new IllegalArgumentException("RUC no puede estar vacío.");
        }

        String rucLimpio = ruc.trim();
        if (!RUC_PATTERN.matcher(rucLimpio).matches()) {
            throw new IllegalArgumentException("El RUC debe contener exactamente 11 dígitos.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 1. Intento API Primaria
        try {
            String primaryUrl = PRIMARY_API_URL + "?numero=" + rucLimpio;
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    primaryUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            if (response.getBody() != null) return response.getBody();
        } catch (Exception e) {
            // Falla silenciosa, pasa al backup
        }

        // 2. Intento API Secundaria (Fallback)
        return consultarRUCEnAPISecundaria(rucLimpio, entity);
    }

    private Map<String, Object> consultarRUCEnAPISecundaria(String rucLimpio, HttpEntity<String> entity) {
        try {
            if (apiPeruToken == null || apiPeruToken.trim().isEmpty()) {
                throw new RuntimeException("API secundaria requiere token no configurado.");
            }
            String secondaryUrl = BACKUP_API_URL + "/ruc/" + rucLimpio + "?token=" + apiPeruToken;
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    secondaryUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            if (response.getBody() != null) return response.getBody();
            throw new RuntimeException("Respuesta vacía de SUNAT.");
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("RUC no encontrado en registros de SUNAT.");
        } catch (Exception e) {
            throw new RuntimeException("Error de conexión con SUNAT.", e);
        }
    }
}
