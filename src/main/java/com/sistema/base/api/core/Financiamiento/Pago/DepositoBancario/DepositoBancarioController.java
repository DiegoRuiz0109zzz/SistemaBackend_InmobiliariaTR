package com.sistema.base.api.core.Financiamiento.Pago.DepositoBancario;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/depositos-bancarios")
@RequiredArgsConstructor
public class DepositoBancarioController {

    private final DepositoBancarioService depositoBancarioService;

    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DepositoBancario>> listarTodos() {
        return ResponseEntity.ok(depositoBancarioService.listarTodos());
    }

    @GetMapping("/recibo/{numeroRecibo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DepositoBancario>> listarPorRecibo(@PathVariable String numeroRecibo) {
        return ResponseEntity.ok(depositoBancarioService.listarPorRecibo(numeroRecibo));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DepositoBancario> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(depositoBancarioService.obtenerPorId(id));
    }
}