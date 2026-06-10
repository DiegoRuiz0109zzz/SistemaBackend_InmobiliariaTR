package com.sistema.base.api.core.Financiamiento.Pago.DepositoBancario;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepositoBancarioService {

    private final DepositoBancarioRepository depositoBancarioRepository;

    @Transactional(readOnly = true)
    public List<DepositoBancario> listarTodos() {
        return depositoBancarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DepositoBancario> listarPorRecibo(String numeroReciboCaja) {
        return depositoBancarioRepository.findByNumeroReciboCaja(numeroReciboCaja);
    }

    @Transactional(readOnly = true)
    public DepositoBancario obtenerPorId(Long id) {
        return depositoBancarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Depósito bancario no encontrado"));
    }
}
