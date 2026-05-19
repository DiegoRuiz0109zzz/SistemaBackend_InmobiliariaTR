package com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial;

import com.sistema.base.api.core.Financiamiento.Contrato.Contrato;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContratoHistorialService {

    private final ContratoHistorialRepository contratoHistorialRepository;

    @Transactional(readOnly = true)
    public List<ContratoHistorial> listarPorContrato(Long contratoId) {
        return contratoHistorialRepository.findByContratoIdOrderByFechaRegistroDesc(contratoId);
    }

    @Transactional(readOnly = true)
    public ContratoHistorial obtenerPorId(Long id) {
        return contratoHistorialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro histórico no encontrado con ID: " + id));
    }

    // ✅ MÉTODO 1: Para hitos normales (Cambios de datos, generación de cuotas, pagos, etc.)
    @Transactional
    public ContratoHistorial registrarHito(Contrato contrato, String tipoRegistro, String descripcion, String observacion) {
        // Llama al método 2 pasando null en la ruta del documento
        return registrarHito(contrato, tipoRegistro, descripcion, observacion, null);
    }

    // ✅ MÉTODO 2: Para cuando SÍ hay un documento de por medio (El ContratoService le pasará la URL aquí)
    @Transactional
    public ContratoHistorial registrarHito(Contrato contrato, String tipoRegistro, String descripcion, String observacion, String rutaDocumentoPdf) {
        ContratoHistorial historial = ContratoHistorial.builder()
                .contrato(contrato)
                .tipoRegistro(tipoRegistro)
                .descripcion(descripcion)
                .observacion(observacion)
                .rutaDocumentoPdf(rutaDocumentoPdf) // ✅ El historial ya sabe dónde está el archivo
                .build();
        return contratoHistorialRepository.save(historial);
    }

    @Transactional
    public void eliminar(Long id) {
        contratoHistorialRepository.deleteById(id);
    }
}