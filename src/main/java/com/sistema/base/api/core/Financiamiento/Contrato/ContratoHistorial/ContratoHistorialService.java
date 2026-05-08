package com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial;

import com.sistema.base.api.core.Financiamiento.Contrato.Contrato;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoRepository;
import com.sistema.base.api.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContratoHistorialService {

    private final ContratoHistorialRepository contratoHistorialRepository;
    private final ContratoRepository contratoRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<ContratoHistorial> listarPorContrato(Long contratoId) {
        return contratoHistorialRepository.findByContratoIdOrderByFechaRegistroDesc(contratoId);
    }

    @Transactional(readOnly = true)
    public ContratoHistorial obtenerPorId(Long id) {
        return contratoHistorialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro histórico no encontrado con ID: " + id));
    }

    // ✅ MÉTODO 1: Actualizado para recibir descripcion (Sistema) y observacion (Manual)
    @Transactional
    public ContratoHistorial registrarHito(Contrato contrato, String tipoRegistro, String descripcion, String observacion) {
        ContratoHistorial historial = ContratoHistorial.builder()
                .contrato(contrato)
                .tipoRegistro(tipoRegistro)
                .descripcion(descripcion) // Texto armado por tu backend (Ej: "Cambió de X a Y")
                .observacion(observacion) // Nota opcional del usuario
                .build();
        return contratoHistorialRepository.save(historial);
    }

    // ✅ MÉTODO 2: Actualizado para quitar el estado y usar descripcion genérica
    @Transactional
    public ContratoHistorial subirContratoPdf(Long contratoId, String tipoRegistro, String observacion, MultipartFile archivo) {
        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));

        if (archivo == null || archivo.isEmpty()) {
            throw new RuntimeException("Debe adjuntar un archivo PDF.");
        }

        String customName = "FIRMADO_" + contrato.getCliente().getNumeroDocumento() + "_" + System.currentTimeMillis() + ".pdf";
        String savedPath = fileStorageService.storeFileWithCustomName(archivo, "contratos_pdf", customName);
        String rutaArchivo = "uploads/" + savedPath;

        ContratoHistorial historial = ContratoHistorial.builder()
                .contrato(contrato)
                .tipoRegistro(tipoRegistro)
                .descripcion("Se subió un documento PDF firmado al sistema.") // Descripción automática
                .observacion(observacion) // Nota del usuario al subir
                .rutaDocumentoPdf(rutaArchivo)
                .build();

        return contratoHistorialRepository.save(historial);
    }

    @Transactional
    public void eliminar(Long id) {
        contratoHistorialRepository.deleteById(id);
    }
}