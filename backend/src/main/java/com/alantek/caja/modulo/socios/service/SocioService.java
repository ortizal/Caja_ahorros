package com.alantek.caja.modulo.socios.service;

import com.alantek.caja.modulo.socios.dto.EstadoCuentaResponse;
import com.alantek.caja.modulo.socios.dto.SocioRequest;
import com.alantek.caja.modulo.socios.dto.SocioResponse;
import com.alantek.caja.modulo.socios.entity.Socio;
import com.alantek.caja.modulo.socios.entity.SocioBeneficiario;
import com.alantek.caja.modulo.socios.repository.SocioRepository;
import com.alantek.caja.shared.audit.AuditService;
import com.alantek.caja.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SocioService {

    private final SocioRepository socioRepository;
    private final AuditService auditService;

    public SocioService(SocioRepository socioRepository, AuditService auditService) {
        this.socioRepository = socioRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SocioResponse> listar(String estado) {
        List<Socio> socios = estado == null || estado.isBlank()
                ? socioRepository.findAll()
                : socioRepository.findAll().stream().filter(s -> estado.equalsIgnoreCase(s.getEstado())).toList();
        return socios.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SocioResponse obtener(Long id) {
        return toResponse(socioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Socio no encontrado: " + id)));
    }

    @Transactional
    public SocioResponse crear(SocioRequest request) {
        if (socioRepository.findByIdentificacion(request.identificacion()).isPresent()) {
            throw new BusinessException("Ya existe un socio con la identificación: " + request.identificacion());
        }

        Socio socio = new Socio();
        socio.setCodigo(request.codigo() != null && !request.codigo().isBlank()
                ? request.codigo()
                : generarCodigo());
        socio.setIdentificacion(request.identificacion());
        socio.setNombres(request.nombres());
        socio.setApellidos(request.apellidos());
        socio.setTelefono(request.telefono());
        socio.setEmail(request.email());
        socio.setDireccion(request.direccion());
        socio.setFechaIngreso(request.fechaIngreso());
        socio.setUsuarioId(request.usuarioId());
        socio.setEstado(request.estado() != null ? request.estado().toUpperCase() : "ACTIVO");
        socio.setBeneficiarios(mapearBeneficiarios(socio, request));

        Socio saved = socioRepository.save(socio);
        auditService.registrar("socios", saved.getId(), "CREAR", null, request);
        return toResponse(saved);
    }

    @Transactional
    public SocioResponse actualizar(Long id, SocioRequest request) {
        Socio socio = socioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Socio no encontrado: " + id));

        socioRepository.findByIdentificacion(request.identificacion())
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new BusinessException("Ya existe un socio con la identificación: " + request.identificacion());
                });

        socio.setIdentificacion(request.identificacion());
        socio.setNombres(request.nombres());
        socio.setApellidos(request.apellidos());
        socio.setTelefono(request.telefono());
        socio.setEmail(request.email());
        socio.setDireccion(request.direccion());
        socio.setFechaIngreso(request.fechaIngreso());
        socio.setUsuarioId(request.usuarioId());
        if (request.estado() != null && !request.estado().isBlank()) {
            socio.setEstado(request.estado().toUpperCase());
        }
        socio.getBeneficiarios().clear();
        socio.getBeneficiarios().addAll(mapearBeneficiarios(socio, request));

        Socio saved = socioRepository.save(socio);
        auditService.registrar("socios", saved.getId(), "EDITAR", null, request);
        return toResponse(saved);
    }

    @Transactional
    public void cambiarEstado(Long id, String estado) {
        String estadoFinal = estado == null ? "ACTIVO" : estado.toUpperCase();
        if (!List.of("ACTIVO", "SUSPENDIDO", "RETIRADO", "FALLECIDO").contains(estadoFinal)) {
            throw new BusinessException("Estado inválido: " + estado);
        }
        Socio socio = socioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Socio no encontrado: " + id));
        String anterior = socio.getEstado();
        socio.setEstado(estadoFinal);
        if ("RETIRADO".equals(estadoFinal)) {
            socio.setFechaRetiro(java.time.LocalDate.now());
        }
        socioRepository.save(socio);
        auditService.registrar("socios", id, "EDITAR", anterior, estadoFinal);
    }

    public EstadoCuentaResponse estadoCuenta(Long id) {
        Socio socio = socioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Socio no encontrado: " + id));
        return new EstadoCuentaResponse(
                socio.getId(),
                socio.getCodigo(),
                socio.getNombres() + " " + socio.getApellidos(),
                socio.getIdentificacion(),
                socio.getEstado(),
                socio.getFechaIngreso(),
                List.of(),
                List.of(),
                List.of());
    }

    private List<SocioBeneficiario> mapearBeneficiarios(Socio socio, SocioRequest request) {
        List<SocioBeneficiario> beneficiarios = new ArrayList<>();
        if (request.beneficiarios() != null) {
            for (SocioRequest.BeneficiarioRequest b : request.beneficiarios()) {
                SocioBeneficiario beneficiario = new SocioBeneficiario();
                beneficiario.setSocio(socio);
                beneficiario.setNombres(b.nombres());
                beneficiario.setParentesco(b.parentesco());
                if (b.porcentaje() != null) {
                    beneficiario.setPorcentaje(b.porcentaje());
                }
                beneficiarios.add(beneficiario);
            }
        }
        return beneficiarios;
    }

    private String generarCodigo() {
        long maxId = socioRepository.maxId() == null ? 0 : socioRepository.maxId();
        String codigo;
        do {
            maxId++;
            codigo = String.format("SOC-%06d", maxId);
        } while (socioRepository.findByCodigo(codigo).isPresent());
        return codigo;
    }

    private SocioResponse toResponse(Socio socio) {
        List<SocioResponse.BeneficiarioResponse> beneficiarios = socio.getBeneficiarios() == null
                ? List.of()
                : socio.getBeneficiarios().stream()
                        .map(b -> new SocioResponse.BeneficiarioResponse(
                                b.getId(), b.getNombres(), b.getParentesco(), b.getPorcentaje()))
                        .toList();
        return new SocioResponse(
                socio.getId(),
                socio.getCodigo(),
                socio.getIdentificacion(),
                socio.getNombres(),
                socio.getApellidos(),
                socio.getTelefono(),
                socio.getEmail(),
                socio.getDireccion(),
                socio.getFechaIngreso(),
                socio.getFechaRetiro(),
                socio.getEstado(),
                socio.getUsuarioId(),
                beneficiarios);
    }
}
