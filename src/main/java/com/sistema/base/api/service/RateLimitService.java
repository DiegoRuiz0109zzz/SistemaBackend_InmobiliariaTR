package com.sistema.base.api.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio para limitar intentos de login por IP.
 * Previene ataques de fuerza bruta limitando a 5 intentos por minuto.
 */
@Service
public class RateLimitService {

    // Almacena intentos por IP: IP -> [contador, timestamp de inicio]
    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();
    
    // Configuración
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MILLIS = 60_000; // 1 minuto

    /**
     * Verifica si una IP puede intentar login.
     * @param clientIp IP del cliente
     * @return true si puede intentar, false si excedió el límite
     */
    public boolean isAllowed(String clientIp) {
        long now = System.currentTimeMillis();
        
        attempts.compute(clientIp, (ip, info) -> {
            if (info == null || now - info.windowStart > WINDOW_MILLIS) {
                // Nueva ventana de tiempo
                return new AttemptInfo(now, 1);
            }
            // Incrementar contador en ventana actual
            info.incrementAttempts();
            return info;
        });
        
        AttemptInfo info = attempts.get(clientIp);
        return info.getAttempts() <= MAX_ATTEMPTS;
    }

    /**
     * Obtiene los segundos restantes hasta que se reinicie la ventana.
     */
    public long getSecondsUntilReset(String clientIp) {
        AttemptInfo info = attempts.get(clientIp);
        if (info == null) return 0;
        
        long elapsed = System.currentTimeMillis() - info.windowStart;
        long remaining = WINDOW_MILLIS - elapsed;
        return Math.max(0, remaining / 1000);
    }

    /**
     * Reinicia el contador para una IP (llamar después de login exitoso).
     */
    public void resetAttempts(String clientIp) {
        attempts.remove(clientIp);
    }

    // Limpieza periódica de entradas antiguas (cada 5 minutos aprox)
    public void cleanupOldEntries() {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(entry -> 
            now - entry.getValue().windowStart > WINDOW_MILLIS * 5
        );
    }

    // Clase interna para almacenar info de intentos
    private static class AttemptInfo {
        final long windowStart;
        private final AtomicInteger attempts;

        AttemptInfo(long windowStart, int initialAttempts) {
            this.windowStart = windowStart;
            this.attempts = new AtomicInteger(initialAttempts);
        }

        void incrementAttempts() {
            attempts.incrementAndGet();
        }

        int getAttempts() {
            return attempts.get();
        }
    }
}
