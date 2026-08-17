package com.alantek.caja.modulo.notificaciones.service;

import com.alantek.caja.modulo.aportaciones.entity.Aportacion;
import com.alantek.caja.modulo.aportaciones.repository.AportacionRepository;
import com.alantek.caja.modulo.caja.entity.CajaApertura;
import com.alantek.caja.modulo.caja.repository.CajaAperturaRepository;
import com.alantek.caja.modulo.creditos.entity.Credito;
import com.alantek.caja.modulo.creditos.entity.CuotaCredito;
import com.alantek.caja.modulo.creditos.repository.CreditoRepository;
import com.alantek.caja.modulo.creditos.repository.CuotaCreditoRepository;
import com.alantek.caja.modulo.notificaciones.dto.NotificacionResponse;
import com.alantek.caja.modulo.notificaciones.entity.Notificacion;
import com.alantek.caja.modulo.notificaciones.repository.NotificacionRepository;
import com.alantek.caja.modulo.socios.entity.Socio;
import com.alantek.caja.modulo.socios.repository.SocioRepository;
import com.alantek.caja.shared.exception.BusinessException;
import com.alantek.caja.shared.security.CurrentUserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final SocioRepository socioRepository;
    private final CreditoRepository creditoRepository;
    private final CuotaCreditoRepository cuotaRepository;
    private final AportacionRepository aportacionRepository;
    private final CajaAperturaRepository cajaAperturaRepository;
    private final CurrentUserService currentUserService;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               SocioRepository socioRepository,
                               CreditoRepository creditoRepository,
                               CuotaCreditoRepository cuotaRepository,
                               AportacionRepository aportacionRepository,
                               CajaAperturaRepository cajaAperturaRepository,
                               CurrentUserService currentUserService) {
        this.notificacionRepository = notificacionRepository;
        this.socioRepository = socioRepository;
        this.creditoRepository = creditoRepository;
        this.cuotaRepository = cuotaRepository;
        this.aportacionRepository = aportacionRepository;
        this.cajaAperturaRepository = cajaAperturaRepository;
        this.currentUserService = currentUserService;
    }

    public List<NotificacionResponse> listarMias() {
        Long usuarioId = currentUserService.requireUserId();
        return notificacionRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public long contarNoLeidas() {
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(currentUserService.requireUserId());
    }

    public NotificacionResponse marcarLeida(Long id) {
        Long usuarioId = currentUserService.requireUserId();
        Notificacion notificacion = notificacionRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new BusinessException("Notificacion no encontrada"));
        notificacion.setLeida(true);
        return toResponse(notificacionRepository.save(notificacion));
    }

    @Transactional
    public void marcarTodasLeidas() {
        Long usuarioId = currentUserService.requireUserId();
        List<Notificacion> noLeidas = notificacionRepository.findByUsuarioIdAndLeidaFalseOrderByCreatedAtDesc(usuarioId);
        noLeidas.forEach(n -> n.setLeida(true));
        notificacionRepository.saveAll(noLeidas);
    }

    public void crear(Long usuarioId, String tipo, String referenciaTabla, Long referenciaId, String mensaje) {
        if (notificacionRepository.existsByUsuarioIdAndTipoAndReferenciaTablaAndReferenciaId(
                usuarioId, tipo, referenciaTabla, referenciaId)) {
            return;
        }
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(usuarioId);
        notificacion.setTipo(tipo);
        notificacion.setReferenciaTabla(referenciaTabla);
        notificacion.setReferenciaId(referenciaId);
        notificacion.setMensaje(mensaje);
        notificacionRepository.save(notificacion);
    }

    @Transactional
    @Scheduled(cron = "0 0 6 * * *")
    public void generarAlertas() {
        LocalDate hoy = LocalDate.now();

        for (CuotaCredito cuota : cuotaRepository.findByEstadoInOrderByFechaVencimientoAsc(List.of("PENDIENTE"))) {
            LocalDate vencimiento = cuota.getFechaVencimiento();
            if (vencimiento.isBefore(hoy) || vencimiento.isAfter(hoy.plusDays(7))) {
                continue;
            }
            Long usuarioId = usuarioDeCredito(cuota.getCreditoId());
            if (usuarioId == null) {
                continue;
            }
            crear(usuarioId, "CUOTA_PROXIMA", "tabla_amortizacion", cuota.getId(),
                    "La cuota #" + cuota.getNumeroCuota() + " del credito #" + cuota.getCreditoId()
                            + " vence el " + vencimiento);
        }

        for (CuotaCredito cuota : cuotaRepository.findByEstadoInOrderByFechaVencimientoAsc(List.of("PENDIENTE", "VENCIDA"))) {
            if (!cuota.getFechaVencimiento().isBefore(hoy)) {
                continue;
            }
            Long usuarioId = usuarioDeCredito(cuota.getCreditoId());
            if (usuarioId == null) {
                continue;
            }
            if (cuota.getMora().compareTo(BigDecimal.ZERO) > 0) {
                crear(usuarioId, "MORA", "tabla_amortizacion", cuota.getId(),
                        "La cuota #" + cuota.getNumeroCuota() + " del credito #" + cuota.getCreditoId()
                                + " tiene mora acumulada de " + cuota.getMora().toPlainString());
            } else {
                crear(usuarioId, "CUOTA_VENCIDA", "tabla_amortizacion", cuota.getId(),
                        "La cuota #" + cuota.getNumeroCuota() + " del credito #" + cuota.getCreditoId()
                                + " esta vencida");
            }
        }

        String periodo = hoy.getYear() + "-" + pad(hoy.getMonthValue());
        for (Aportacion aportacion : aportacionRepository.findByPeriodoOrderBySocioId(periodo)) {
            if (!"PENDIENTE".equals(aportacion.getEstado())) {
                continue;
            }
            Long usuarioId = usuarioDeSocio(aportacion.getSocioId());
            if (usuarioId == null) {
                continue;
            }
            crear(usuarioId, "APORTACION_PENDIENTE", "aportaciones", aportacion.getId(),
                    "La aportacion del periodo " + periodo + " esta pendiente por "
                            + aportacion.getMontoEsperado().toPlainString());
        }

        for (CajaApertura caja : cajaAperturaRepository.findByEstadoOrderByOpenedAtAsc("ABIERTA")) {
            if (!caja.getFecha().isBefore(hoy)) {
                continue;
            }
            crear(caja.getCajeroId(), "CIERRE_PENDIENTE", "caja_apertura", caja.getId(),
                    "La caja del " + caja.getFecha() + " sigue abierta, realice el cierre");
        }
    }

    private Long usuarioDeCredito(Long creditoId) {
        return creditoRepository.findById(creditoId)
                .map(Credito::getSocioId)
                .map(this::usuarioDeSocio)
                .orElse(null);
    }

    private Long usuarioDeSocio(Long socioId) {
        return socioRepository.findById(socioId)
                .map(Socio::getUsuarioId)
                .orElse(null);
    }

    private String pad(int valor) {
        return valor < 10 ? "0" + valor : String.valueOf(valor);
    }

    private NotificacionResponse toResponse(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getId(),
                notificacion.getTipo(),
                notificacion.getReferenciaTabla(),
                notificacion.getReferenciaId(),
                notificacion.getMensaje(),
                notificacion.isLeida(),
                notificacion.getCreatedAt());
    }
}
