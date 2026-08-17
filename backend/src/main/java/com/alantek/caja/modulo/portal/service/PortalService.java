package com.alantek.caja.modulo.portal.service;

import com.alantek.caja.modulo.ahorros.dto.CuentaAhorroResponse;
import com.alantek.caja.modulo.ahorros.dto.MovimientoAhorroResponse;
import com.alantek.caja.modulo.ahorros.service.AhorroService;
import com.alantek.caja.modulo.aportaciones.dto.AportacionPagoResponse;
import com.alantek.caja.modulo.aportaciones.dto.AportacionResponse;
import com.alantek.caja.modulo.aportaciones.service.AportacionService;
import com.alantek.caja.modulo.creditos.dto.CreditoResponse;
import com.alantek.caja.modulo.creditos.dto.CuotaCreditoResponse;
import com.alantek.caja.modulo.creditos.dto.PagoCuotaResponse;
import com.alantek.caja.modulo.creditos.service.CreditoService;
import com.alantek.caja.modulo.notificaciones.service.NotificacionService;
import com.alantek.caja.modulo.portal.dto.PortalAhorroResponse;
import com.alantek.caja.modulo.portal.dto.PortalAportacionResponse;
import com.alantek.caja.modulo.portal.dto.PortalCreditoResponse;
import com.alantek.caja.modulo.portal.dto.PortalResumenResponse;
import com.alantek.caja.modulo.portal.dto.PortalSocioResponse;
import com.alantek.caja.modulo.socios.entity.Socio;
import com.alantek.caja.modulo.socios.repository.SocioRepository;
import com.alantek.caja.shared.exception.BusinessException;
import com.alantek.caja.shared.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PortalService {

    private final SocioRepository socioRepository;
    private final CurrentUserService currentUserService;
    private final AhorroService ahorroService;
    private final AportacionService aportacionService;
    private final CreditoService creditoService;
    private final NotificacionService notificacionService;

    public PortalService(SocioRepository socioRepository,
                         CurrentUserService currentUserService,
                         AhorroService ahorroService,
                         AportacionService aportacionService,
                         CreditoService creditoService,
                         NotificacionService notificacionService) {
        this.socioRepository = socioRepository;
        this.currentUserService = currentUserService;
        this.ahorroService = ahorroService;
        this.aportacionService = aportacionService;
        this.creditoService = creditoService;
        this.notificacionService = notificacionService;
    }

    private Socio socioAutenticado() {
        Long usuarioId = currentUserService.requireUserId();
        List<Socio> socios = socioRepository.findByUsuarioId(usuarioId);
        if (socios.isEmpty()) {
            throw new BusinessException("No existe un socio vinculado a su usuario");
        }
        if (socios.size() > 1) {
            throw new BusinessException("Existen multiples socios vinculados a su usuario");
        }
        return socios.get(0);
    }

    @Transactional(readOnly = true)
    public PortalResumenResponse resumen() {
        Socio socio = socioAutenticado();

        List<CuentaAhorroResponse> cuentas = ahorroService.listarCuentas(socio.getId());
        BigDecimal saldoAhorro = cuentas.stream()
                .map(CuentaAhorroResponse::saldo)
                .filter(c -> c != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AportacionResponse> aportaciones = aportacionService.listarAportaciones(null, socio.getId());
        BigDecimal totalAportado = aportaciones.stream()
                .map(AportacionResponse::montoPagado)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String periodoActual = periodoActual();
        BigDecimal aportePendientePeriodo = aportaciones.stream()
                .filter(a -> periodoActual.equals(a.periodo()) && !"PAGADA".equals(a.estado()))
                .map(a -> a.montoEsperado().subtract(nvl(a.montoPagado())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CreditoResponse> creditos = creditoService.listarCreditos(socio.getId());
        BigDecimal saldoCreditoVigente = creditos.stream()
                .filter(c -> "VIGENTE".equals(c.estado()) || "EN_MORA".equals(c.estado()))
                .map(CreditoResponse::saldoCapital)
                .filter(c -> c != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int cuotasVencidas = 0;
        int cuotasPendientes = 0;
        for (CreditoResponse credito : creditos) {
            for (CuotaCreditoResponse cuota : creditoService.listarCuotas(credito.id())) {
                if ("VENCIDA".equals(cuota.estado())) {
                    cuotasVencidas++;
                } else if ("PENDIENTE".equals(cuota.estado())) {
                    cuotasPendientes++;
                }
            }
        }

        return new PortalResumenResponse(
                toSocioResponse(socio),
                saldoAhorro,
                totalAportado,
                aportePendientePeriodo,
                saldoCreditoVigente,
                cuotasVencidas,
                cuotasPendientes,
                notificacionService.contarNoLeidas());
    }

    @Transactional(readOnly = true)
    public List<PortalAhorroResponse> ahorro() {
        Socio socio = socioAutenticado();
        return ahorroService.listarCuentas(socio.getId()).stream()
                .map(cuenta -> new PortalAhorroResponse(cuenta, movimientos(cuenta.id())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PortalAportacionResponse> aportaciones() {
        Socio socio = socioAutenticado();
        return aportacionService.listarAportaciones(null, socio.getId()).stream()
                .map(aportacion -> new PortalAportacionResponse(aportacion, pagos(aportacion.id())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PortalCreditoResponse> creditos() {
        Socio socio = socioAutenticado();
        return creditoService.listarCreditos(socio.getId()).stream()
                .map(credito -> new PortalCreditoResponse(credito,
                        creditoService.listarCuotas(credito.id()),
                        creditoService.listarPagos(credito.id())))
                .toList();
    }

    private List<MovimientoAhorroResponse> movimientos(Long cuentaId) {
        return ahorroService.listarMovimientos(cuentaId);
    }

    private List<AportacionPagoResponse> pagos(Long aportacionId) {
        return aportacionService.listarPagos(aportacionId);
    }

    private PortalSocioResponse toSocioResponse(Socio socio) {
        return new PortalSocioResponse(
                socio.getId(),
                socio.getCodigo(),
                socio.getIdentificacion(),
                socio.getNombres(),
                socio.getApellidos(),
                socio.getEstado(),
                socio.getFechaIngreso());
    }

    private String periodoActual() {
        LocalDate hoy = LocalDate.now();
        return hoy.getYear() + "-" + (hoy.getMonthValue() < 10 ? "0" : "") + hoy.getMonthValue();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
