package com.alantek.caja.modulo.bancos.service;

import com.alantek.caja.modulo.bancos.dto.ConciliacionRequest;
import com.alantek.caja.modulo.bancos.dto.ConciliacionResponse;
import com.alantek.caja.modulo.bancos.dto.CuentaBancariaRequest;
import com.alantek.caja.modulo.bancos.dto.CuentaBancariaResponse;
import com.alantek.caja.modulo.bancos.dto.BancoMovimientoRequest;
import com.alantek.caja.modulo.bancos.dto.BancoMovimientoResponse;
import com.alantek.caja.modulo.bancos.entity.BancoMovimiento;
import com.alantek.caja.modulo.bancos.entity.ConciliacionBancaria;
import com.alantek.caja.modulo.bancos.entity.CuentaBancaria;
import com.alantek.caja.modulo.bancos.repository.BancoMovimientoRepository;
import com.alantek.caja.modulo.bancos.repository.ConciliacionBancariaRepository;
import com.alantek.caja.modulo.bancos.repository.CuentaBancariaRepository;
import com.alantek.caja.shared.audit.AuditService;
import com.alantek.caja.shared.exception.BusinessException;
import com.alantek.caja.shared.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BancoService {

    private final CuentaBancariaRepository cuentaRepository;
    private final BancoMovimientoRepository movimientoRepository;
    private final ConciliacionBancariaRepository conciliacionRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public BancoService(CuentaBancariaRepository cuentaRepository,
                        BancoMovimientoRepository movimientoRepository,
                        ConciliacionBancariaRepository conciliacionRepository,
                        CurrentUserService currentUserService,
                        AuditService auditService) {
        this.cuentaRepository = cuentaRepository;
        this.movimientoRepository = movimientoRepository;
        this.conciliacionRepository = conciliacionRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    public List<CuentaBancariaResponse> listarCuentas() {
        return cuentaRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CuentaBancariaResponse crearCuenta(CuentaBancariaRequest request) {
        CuentaBancaria cuenta = new CuentaBancaria();
        cuenta.setBanco(request.banco());
        cuenta.setNumeroCuenta(request.numeroCuenta());
        cuenta.setTipo(request.tipo());
        cuenta.setSaldoContable(request.saldoContable() == null ? BigDecimal.ZERO : request.saldoContable());
        CuentaBancaria saved = cuentaRepository.save(cuenta);
        auditService.registrar("cuenta_bancaria", saved.getId(), "CREAR", null, request);
        return toResponse(saved);
    }

    @Transactional
    public BancoMovimientoResponse registrarMovimiento(Long cuentaId, BancoMovimientoRequest request) {
        CuentaBancaria cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new BusinessException("Cuenta bancaria no encontrada: " + cuentaId));

        String tipo = request.tipo().toUpperCase();
        BigDecimal monto = request.monto();
        boolean incrementa = esIngreso(tipo);
        if (!incrementa && monto.compareTo(cuenta.getSaldoContable()) > 0) {
            throw new BusinessException("Saldo insuficiente en la cuenta bancaria");
        }

        cuenta.setSaldoContable(incrementa
                ? cuenta.getSaldoContable().add(monto)
                : cuenta.getSaldoContable().subtract(monto));
        cuentaRepository.save(cuenta);

        BancoMovimiento movimiento = new BancoMovimiento();
        movimiento.setCuentaBancariaId(cuentaId);
        movimiento.setTipo(tipo);
        movimiento.setMonto(monto);
        movimiento.setFecha(request.fecha());
        movimiento.setComprobanteId(request.comprobanteId());

        BancoMovimiento saved = movimientoRepository.save(movimiento);
        auditService.registrar("banco_movimiento", saved.getId(), "CREAR", null, request);
        return toResponse(saved, cuenta);
    }

    public List<BancoMovimientoResponse> listarMovimientos(Long cuentaId) {
        return movimientoRepository.findByCuentaBancariaIdOrderByFechaAsc(cuentaId).stream()
                .map(m -> toResponse(m, cuentaRepository.findById(cuentaId).orElse(null)))
                .toList();
    }

    @Transactional
    public ConciliacionResponse conciliar(Long cuentaId, ConciliacionRequest request) {
        CuentaBancaria cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new BusinessException("Cuenta bancaria no encontrada: " + cuentaId));

        BigDecimal saldoContable = cuenta.getSaldoContable();
        BigDecimal saldoBancario = request.saldoBancario();
        BigDecimal diferencia = saldoContable.subtract(saldoBancario);

        ConciliacionBancaria conciliacion = new ConciliacionBancaria();
        conciliacion.setCuentaBancariaId(cuentaId);
        conciliacion.setPeriodo(request.periodo());
        conciliacion.setSaldoContable(saldoContable);
        conciliacion.setSaldoBancario(saldoBancario);
        conciliacion.setDiferencia(diferencia);
        conciliacion.setRealizadoPor(currentUserService.requireUserId());

        ConciliacionBancaria saved = conciliacionRepository.save(conciliacion);
        auditService.registrar("conciliacion_bancaria", saved.getId(), "CREAR", null, request);
        return new ConciliacionResponse(saved.getId(), cuentaId, request.periodo(),
                saldoContable, saldoBancario, diferencia);
    }

    private boolean esIngreso(String tipo) {
        return List.of("DEPOSITO", "NOTA_CREDITO").contains(tipo);
    }

    private CuentaBancariaResponse toResponse(CuentaBancaria cuenta) {
        return new CuentaBancariaResponse(cuenta.getId(), cuenta.getBanco(), cuenta.getNumeroCuenta(),
                cuenta.getTipo(), cuenta.getSaldoContable());
    }

    private BancoMovimientoResponse toResponse(BancoMovimiento movimiento, CuentaBancaria cuenta) {
        return new BancoMovimientoResponse(movimiento.getId(), movimiento.getCuentaBancariaId(),
                movimiento.getTipo(), movimiento.getMonto(), movimiento.getFecha(),
                movimiento.getComprobanteId(), movimiento.getConciliado(),
                cuenta != null ? cuenta.getSaldoContable() : null);
    }
}
