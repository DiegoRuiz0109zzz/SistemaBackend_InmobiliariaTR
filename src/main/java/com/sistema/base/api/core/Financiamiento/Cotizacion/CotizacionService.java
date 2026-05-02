package com.sistema.base.api.core.Financiamiento.Cotizacion;

import com.sistema.base.api.core.Financiamiento.Cotizacion.dtos.CotizacionRequest;
import com.sistema.base.api.core.Lotizacion.Lote.Lote;
import com.sistema.base.api.core.Lotizacion.Lote.LoteRepository;
import com.sistema.base.api.core.Usuario.Interesados.Interesado;
import com.sistema.base.api.core.Usuario.Interesados.InteresadoRepository;
import com.sistema.base.api.core.Vendedores.Vendedor;
import com.sistema.base.api.core.Vendedores.VendedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final LoteRepository loteRepository;
    private final InteresadoRepository interesadoRepository;
    private final VendedorRepository vendedorRepository;

    @Transactional
    public void verificarExpiradas() {
        List<Cotizacion> todas = cotizacionRepository.findAll();
        LocalDate hoy = LocalDate.now();
        for (Cotizacion c : todas) {
            if (c.getEstado() == EstadoCotizacion.VIGENTE && c.getFechaValidez().isBefore(hoy)) {
                c.setEstado(EstadoCotizacion.EXPIRADA);
                cotizacionRepository.save(c);
            }
        }
    }

    public List<Cotizacion> listarTodas() {
        return cotizacionRepository.findByEnabledTrueOrderByFechaCotizacionDesc();
    }

    public List<Cotizacion> buscarPorDniInteresado(String dni) {
        return cotizacionRepository.findByInteresadoNumeroDocumentoAndEnabledTrueOrderByFechaCotizacionDesc(dni);
    }

    @Transactional
    public Cotizacion crearCotizacion(CotizacionRequest req) {
        Lote lote = loteRepository.findById(req.getLoteId())
                .orElseThrow(() -> new RuntimeException("Lote no encontrado"));
        Interesado interesado = interesadoRepository.findById(req.getInteresadoId())
                .orElseThrow(() -> new RuntimeException("Interesado no encontrado"));
        Vendedor vendedor = vendedorRepository.findById(req.getVendedorId())
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado"));

        Interesado coComprador = null;
        if (req.getCoCompradorId() != null) {
            coComprador = interesadoRepository.findById(req.getCoCompradorId())
                    .orElseThrow(() -> new RuntimeException("Co-comprador (Interesado) no encontrado"));
        }

        int diasValidez = (req.getDiasValidez() != null && req.getDiasValidez() > 0) ? req.getDiasValidez() : 7;
        LocalDate hoy = LocalDate.now();

        Cotizacion cotizacion = Cotizacion.builder()
                .lote(lote)
                .interesado(interesado)
                .vendedor(vendedor)
                .coComprador(coComprador)
                // --- APLICAMOS LOS NUEVOS CAMPOS AQUÍ ---
                .tipoInicial(req.getTipoInicial())
                .cuotasFlexibles(req.getCuotasFlexibles() != null ? req.getCuotasFlexibles() : false)
                .precioTotal(req.getPrecioTotal())
                .montoInicialAcordado(req.getMontoInicialAcordado())
                .cantidadCuotas(req.getCantidadCuotas())
                .cuotasEspeciales(req.getCuotasEspeciales())
                .montoCuotaEspecial(req.getMontoCuotaEspecial())
                .montoCuotaCotizacion(req.getMontoCuotaCotizacion())
                .saldoFinanciar(req.getSaldoFinanciar())
                .fechaCotizacion(hoy)
                .fechaValidez(hoy.plusDays(diasValidez))
                .estado(EstadoCotizacion.VIGENTE)
                .enabled(true)
                .build();

        return cotizacionRepository.save(cotizacion);
    }

    @Transactional
    public Cotizacion marcarComoConvertida(Long id) {
        Cotizacion cotizacion = cotizacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cotización no encontrada"));
        cotizacion.setEstado(EstadoCotizacion.CONVERTIDA_A_CONTRATO);
        return cotizacionRepository.save(cotizacion);
    }
}