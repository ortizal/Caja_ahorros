package com.alantek.caja.config;

import com.alantek.caja.modulo.ahorros.entity.ProductoAhorro;
import com.alantek.caja.modulo.ahorros.repository.ProductoAhorroRepository;
import com.alantek.caja.modulo.creditos.entity.ProductoCredito;
import com.alantek.caja.modulo.creditos.repository.ProductoCreditoRepository;
import com.alantek.caja.modulo.aportaciones.entity.AportacionConfig;
import com.alantek.caja.modulo.aportaciones.repository.AportacionConfigRepository;
import com.alantek.caja.modulo.contabilidad.entity.PlanCuenta;
import com.alantek.caja.modulo.contabilidad.entity.ReglaContable;
import com.alantek.caja.modulo.contabilidad.repository.PlanCuentaRepository;
import com.alantek.caja.modulo.contabilidad.repository.ReglaContableRepository;
import com.alantek.caja.modulo.reportes.entity.Reporte;
import com.alantek.caja.modulo.reportes.repository.ReporteRepository;
import com.alantek.caja.modulo.seguridad.entity.Permiso;
import com.alantek.caja.modulo.seguridad.entity.Rol;
import com.alantek.caja.modulo.seguridad.entity.Usuario;
import com.alantek.caja.modulo.seguridad.repository.PermisoRepository;
import com.alantek.caja.modulo.seguridad.repository.RolRepository;
import com.alantek.caja.modulo.seguridad.repository.UsuarioRepository;
import com.alantek.caja.modulo.socios.entity.Socio;
import com.alantek.caja.modulo.socios.repository.SocioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final LocalDate VIGENCIA = LocalDate.of(2024, 1, 1);

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlanCuentaRepository planCuentaRepository;
    private final ReglaContableRepository reglaContableRepository;
    private final AportacionConfigRepository aportacionConfigRepository;
    private final ProductoAhorroRepository productoAhorroRepository;
    private final ProductoCreditoRepository productoCreditoRepository;
    private final SocioRepository socioRepository;
    private final ReporteRepository reporteRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(RolRepository rolRepository,
                      PermisoRepository permisoRepository,
                      UsuarioRepository usuarioRepository,
                      PlanCuentaRepository planCuentaRepository,
                      ReglaContableRepository reglaContableRepository,
                      AportacionConfigRepository aportacionConfigRepository,
                      ProductoAhorroRepository productoAhorroRepository,
                      ProductoCreditoRepository productoCreditoRepository,
                      SocioRepository socioRepository,
                      ReporteRepository reporteRepository,
                      PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.usuarioRepository = usuarioRepository;
        this.planCuentaRepository = planCuentaRepository;
        this.reglaContableRepository = reglaContableRepository;
        this.aportacionConfigRepository = aportacionConfigRepository;
        this.productoAhorroRepository = productoAhorroRepository;
        this.productoCreditoRepository = productoCreditoRepository;
        this.socioRepository = socioRepository;
        this.reporteRepository = reporteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        sembrarPermisos();
        sembrarRoles();
        sembrarUsuarios();
        sembrarPortal();
        sembrarPlanCuentas();
        sembrarReglasContables();
        sembrarAportacionesYahorros();
        sembrarCreditos();
        sembrarReportes();
    }

    private void sembrarPermisos() {
        List<String> modulos = List.of("SEGURIDAD", "SOCIOS", "CAJA", "BANCOS", "CONTABILIDAD", "APORTACIONES", "AHORROS", "CREDITOS", "TESORERIA", "PORTAL");
        List<String> acciones = List.of("VER", "CREAR", "EDITAR", "APROBAR", "ANULAR");
        for (String modulo : modulos) {
            for (String accion : acciones) {
                if (permisoRepository.findByModuloAndAccion(modulo, accion).isEmpty()) {
                    Permiso permiso = new Permiso();
                    permiso.setModulo(modulo);
                    permiso.setAccion(accion);
                    permisoRepository.save(permiso);
                }
            }
        }
    }

    private void sembrarRoles() {
        Map<String, String> roles = new LinkedHashMap<>();
        roles.put("ADMIN", "Administración total del sistema");
        roles.put("GERENTE", "Visión gerencial y aprobaciones de alto nivel");
        roles.put("CONTADOR", "Gestión contable: plan de cuentas, asientos y cierres");
        roles.put("TESORERO", "Operación diaria de caja: apertura, cobros y depósitos");
        roles.put("CREDITO", "Gestión de cartera y cobranza");
        roles.put("AUDITOR", "Solo consulta y auditoría");
        roles.put("SOCIO", "Portal de lectura del socio (consulta de ahorro, aportes y créditos)");

        for (Map.Entry<String, String> entry : roles.entrySet()) {
            Rol rol = rolRepository.findByNombre(entry.getKey()).orElseGet(() -> {
                Rol nuevo = new Rol();
                nuevo.setNombre(entry.getKey());
                nuevo.setDescripcion(entry.getValue());
                return nuevo;
            });
            rol.setPermisos(permisosDeRol(entry.getKey()));
            rolRepository.save(rol);
        }
    }

    private Set<Permiso> permisosDeRol(String rol) {
        Set<String> autoridades = switch (rol) {
            case "ADMIN" -> permisoRepository.findAll().stream()
                    .map(p -> p.getModulo() + ":" + p.getAccion())
                    .collect(Collectors.toSet());
            case "CONTADOR" -> Set.of(
                    "CONTABILIDAD:VER", "CONTABILIDAD:CREAR", "CONTABILIDAD:EDITAR",
                    "CONTABILIDAD:APROBAR", "CONTABILIDAD:ANULAR",
                    "BANCOS:VER", "BANCOS:CREAR", "BANCOS:EDITAR", "BANCOS:APROBAR", "BANCOS:ANULAR",
                    "SOCIOS:VER", "CAJA:VER",
                    "APORTACIONES:VER", "AHORROS:VER", "CREDITOS:VER",
                    "TESORERIA:VER", "TESORERIA:CREAR");
            case "TESORERO" -> Set.of(
                    "CAJA:VER", "CAJA:CREAR", "CAJA:EDITAR", "CAJA:APROBAR", "CAJA:ANULAR",
                    "BANCOS:VER", "BANCOS:CREAR", "BANCOS:EDITAR", "BANCOS:APROBAR", "BANCOS:ANULAR",
                    "SOCIOS:VER", "CONTABILIDAD:VER",
                    "APORTACIONES:VER", "APORTACIONES:CREAR", "APORTACIONES:EDITAR",
                    "APORTACIONES:APROBAR", "APORTACIONES:ANULAR",
                    "AHORROS:VER", "AHORROS:CREAR", "AHORROS:EDITAR", "AHORROS:APROBAR", "AHORROS:ANULAR",
                    "CREDITOS:VER", "CREDITOS:CREAR",
                    "TESORERIA:VER", "TESORERIA:CREAR", "TESORERIA:EDITAR", "TESORERIA:ANULAR");
            case "GERENTE" -> Set.of(
                    "SOCIOS:VER", "SOCIOS:CREAR", "SOCIOS:EDITAR", "SOCIOS:APROBAR", "SOCIOS:ANULAR",
                    "CONTABILIDAD:VER", "CONTABILIDAD:APROBAR",
                    "CAJA:VER", "BANCOS:VER", "SEGURIDAD:VER",
                    "APORTACIONES:VER", "AHORROS:VER", "CREDITOS:VER", "CREDITOS:APROBAR",
                    "TESORERIA:VER", "TESORERIA:APROBAR");
            case "CREDITO" -> Set.of(
                    "SOCIOS:VER", "CAJA:VER", "CAJA:CREAR", "CONTABILIDAD:VER",
                    "CREDITOS:VER", "CREDITOS:CREAR", "CREDITOS:EDITAR",
                    "CREDITOS:APROBAR", "CREDITOS:ANULAR");
            case "AUDITOR" -> Set.of(
                    "SOCIOS:VER", "CAJA:VER", "BANCOS:VER", "CONTABILIDAD:VER", "SEGURIDAD:VER",
                    "APORTACIONES:VER", "AHORROS:VER", "CREDITOS:VER", "TESORERIA:VER");
            case "SOCIO" -> Set.of("PORTAL:VER");
            default -> Set.of();
        };

        Set<Permiso> permisos = new HashSet<>();
        for (String autoridad : autoridades) {
            int sep = autoridad.indexOf(':');
            permisoRepository.findByModuloAndAccion(
                            autoridad.substring(0, sep), autoridad.substring(sep + 1))
                    .ifPresent(permisos::add);
        }
        return permisos;
    }

    private void sembrarUsuarios() {
        crearUsuarioSiNoExiste("admin", "admin1234", "Administrador ALANTEK", Set.of("ADMIN"));
        crearUsuarioSiNoExiste("gerente", "gerente123", "Gerente General", Set.of("GERENTE"));
        crearUsuarioSiNoExiste("contador", "contador123", "Contador General", Set.of("CONTADOR"));
        crearUsuarioSiNoExiste("cajero", "cajero123", "Cajero Principal", Set.of("TESORERO"));
        crearUsuarioSiNoExiste("credito", "credito123", "Analista de Credito", Set.of("CREDITO"));
        actualizarPasswordSiExiste("admin", "admin1234");
    }

    private void crearUsuarioSiNoExiste(String username, String password, String nombreCompleto, Set<String> roles) {
        if (usuarioRepository.findByUsername(username).isPresent()) {
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setNombreCompleto(nombreCompleto);
        usuario.setEstado("ACTIVO");
        Set<Rol> rolesSet = roles.stream()
                .map(nombre -> rolRepository.findByNombre(nombre).orElseThrow())
                .collect(Collectors.toSet());
        usuario.setRoles(rolesSet);
        usuarioRepository.save(usuario);
    }

    private void actualizarPasswordSiExiste(String username, String password) {
        usuarioRepository.findByUsername(username).ifPresent(usuario -> {
            usuario.setPasswordHash(passwordEncoder.encode(password));
            usuarioRepository.save(usuario);
        });
    }

    private void sembrarPortal() {
        crearUsuarioSiNoExiste("socio", "socio123", "Maria Perez", Set.of("SOCIO"));
        if (socioRepository.findByCodigo("SOC-DEMO-01").isEmpty()) {
            Long usuarioId = usuarioRepository.findByUsername("socio").orElseThrow().getId();
            Socio socio = new Socio();
            socio.setCodigo("SOC-DEMO-01");
            socio.setIdentificacion("9999999999");
            socio.setNombres("Maria");
            socio.setApellidos("Perez");
            socio.setFechaIngreso(LocalDate.now());
            socio.setEstado("ACTIVO");
            socio.setUsuarioId(usuarioId);
            socioRepository.save(socio);
        }
    }

    private void sembrarPlanCuentas() {
        crearCuenta("1", "ACTIVO", null, 1, false);
        crearCuenta("2", "PASIVO", null, 1, false);
        crearCuenta("3", "PATRIMONIO", null, 1, false);
        crearCuenta("4", "INGRESO", null, 1, false);
        crearCuenta("5", "GASTO", null, 1, false);

        PlanCuenta caja = crearCuenta("1.1", "Caja", codigoId("1").getId(), 2, false);
        PlanCuenta bancos = crearCuenta("1.2", "Bancos", codigoId("1").getId(), 2, false);
        PlanCuenta cartera = crearCuenta("1.3", "Cartera de créditos", codigoId("1").getId(), 2, false);
        PlanCuenta cxc = crearCuenta("1.4", "Cuentas por cobrar", codigoId("1").getId(), 2, false);

        PlanCuenta pasivo = codigoId("2");
        PlanCuenta obligaciones = crearCuenta("2.1", "Obligaciones con socios", pasivo.getId(), 2, false);
        crearCuenta("2.2", "Cuentas por pagar", pasivo.getId(), 2, false);

        PlanCuenta patrimonio = codigoId("3");
        crearCuenta("3.1", "Capital y reservas", patrimonio.getId(), 2, false);

        PlanCuenta ingreso = codigoId("4");
        PlanCuenta ingresosFinancieros = crearCuenta("4.1", "Ingresos financieros", ingreso.getId(), 2, false);

        PlanCuenta gasto = codigoId("5");
        PlanCuenta gastosOperativos = crearCuenta("5.1", "Gastos operativos", gasto.getId(), 2, false);

        crearCuenta("1.1.01", "Caja General", caja.getId(), 3, true);
        crearCuenta("1.2.01", "Banco - Cuenta corriente", bancos.getId(), 3, true);
        crearCuenta("1.3.01", "Cartera de créditos vigente", cartera.getId(), 3, true);
        crearCuenta("1.4.01", "Cuentas por cobrar", cxc.getId(), 3, true);
        crearCuenta("2.1.01", "Aportes de socios", obligaciones.getId(), 3, true);
        crearCuenta("2.1.02", "Ahorros de socios", obligaciones.getId(), 3, true);
        crearCuenta("2.2.01", "Cuentas por pagar", codigoId("2.2").getId(), 3, true);
        crearCuenta("3.1.01", "Reserva legal", codigoId("3.1").getId(), 3, true);
        crearCuenta("4.1.01", "Ingreso por intereses", ingresosFinancieros.getId(), 3, true);
        crearCuenta("4.1.02", "Ingreso por mora", ingresosFinancieros.getId(), 3, true);
        crearCuenta("5.1.01", "Gastos administrativos", gastosOperativos.getId(), 3, true);
        crearCuenta("5.1.02", "Gastos por intereses", gastosOperativos.getId(), 3, true);
    }

    private PlanCuenta crearCuenta(String codigo, String nombre, Long padreId, int nivel, boolean aceptaMovimiento) {
        return planCuentaRepository.findByCodigo(codigo).orElseGet(() -> {
            PlanCuenta cuenta = new PlanCuenta();
            cuenta.setCodigo(codigo);
            cuenta.setNombre(nombre);
            cuenta.setTipo(tipoPorCodigo(codigo));
            cuenta.setCuentaPadreId(padreId);
            cuenta.setNivel(nivel);
            cuenta.setAceptaMovimiento(aceptaMovimiento);
            return planCuentaRepository.save(cuenta);
        });
    }

    private String tipoPorCodigo(String codigo) {
        return switch (codigo.charAt(0)) {
            case '1' -> "ACTIVO";
            case '2' -> "PASIVO";
            case '3' -> "PATRIMONIO";
            case '4' -> "INGRESO";
            default -> "GASTO";
        };
    }

    private PlanCuenta codigoId(String codigo) {
        return planCuentaRepository.findByCodigo(codigo).orElseThrow();
    }

    private void sembrarReglasContables() {
        regla("APORTACION", "1.1.01", "2.1.01");
        regla("DEPOSITO_AHORRO", "1.1.01", "2.1.02");
        regla("RETIRO_AHORRO", "2.1.02", "1.1.01");
        regla("INTERES_AHORRO", "5.1.02", "2.1.02");
        regla("DESEMBOLSO_CREDITO", "1.3.01", "1.1.01");
        regla("PAGO_CAPITAL", "1.1.01", "1.3.01");
        regla("PAGO_INTERES", "1.1.01", "4.1.01");
        regla("PAGO_MORA", "1.1.01", "4.1.02");
        regla("GASTO_PAGADO", "5.1.01", "1.1.01");
        regla("COBRO_CUENTA", "1.1.01", "1.4.01");
    }

    private void regla(String operacion, String codigoDebe, String codigoHaber) {
        if (reglaContableRepository.findByOperacion(operacion).isPresent()) {
            return;
        }
        ReglaContable regla = new ReglaContable();
        regla.setOperacion(operacion);
        regla.setCuentaDebeId(codigoId(codigoDebe).getId());
        regla.setCuentaHaberId(codigoId(codigoHaber).getId());
        regla.setVigenteDesde(VIGENCIA);
        regla.setVigenteHasta(null);
        regla.setActivo(true);
        reglaContableRepository.save(regla);
    }

    private void sembrarAportacionesYahorros() {
        if (aportacionConfigRepository.count() == 0) {
            AportacionConfig config = new AportacionConfig();
            config.setTipo("OBLIGATORIA");
            config.setModoCalculo("FIJO");
            config.setValor(new BigDecimal("25.0000"));
            config.setPeriodicidad("MENSUAL");
            config.setMontoMinimo(new BigDecimal("10.00"));
            config.setMontoMaximo(new BigDecimal("200.00"));
            config.setVigenteDesde(VIGENCIA);
            config.setVigenteHasta(null);
            aportacionConfigRepository.save(config);
        }
        if (productoAhorroRepository.count() == 0) {
            ProductoAhorro producto = new ProductoAhorro();
            producto.setNombre("A LA VISTA");
            producto.setTasaInteres(new BigDecimal("2.5000"));
            producto.setPeriodicidadCapitalizacion("MENSUAL");
            producto.setSaldoMinimo(BigDecimal.ZERO);
            producto.setVigenteDesde(VIGENCIA);
            producto.setVigenteHasta(null);
            producto.setActivo(true);
            productoAhorroRepository.save(producto);
        }
    }

    private void sembrarCreditos() {
        if (productoCreditoRepository.count() == 0) {
            ProductoCredito producto = new ProductoCredito();
            producto.setNombre("CREDITO PERSONAL");
            producto.setTasaInteres(new BigDecimal("18.0000"));
            producto.setTasaMora(new BigDecimal("1.0000"));
            producto.setSistemaAmortizacion("FRANCES");
            producto.setPlazoMaxMeses(36);
            producto.setMontoMin(new BigDecimal("100.00"));
            producto.setMontoMax(new BigDecimal("5000.00"));
            producto.setRequiereGarante(false);
            producto.setVigenteDesde(VIGENCIA);
            producto.setVigenteHasta(null);
            producto.setActivo(true);
            productoCreditoRepository.save(producto);
        }
    }

    private void sembrarReportes() {
        List<ReportePlantilla> plantillas = List.of(
                plantilla("socios", "Listado de Socios", "SOCIO", "portrait",
                        "Reporte del registro completo de socios activos e inactivos"),
                plantilla("caja", "Arqueo de Caja", "CAJA", "portrait",
                        "Reporte diario de arqueo de caja con saldos y movimientos"),
                plantilla("cartera", "Cartera de Creditos", "CREDITO", "landscape",
                        "Estado de cartera de creditos con cuotas vencidas"),
                plantilla("ahorros", "Ahorros de Socios", "AHORRO", "portrait",
                        "Reporte de cuentas de ahorro de socios"),
                plantilla("aportaciones", "Aportaciones de Socios", "APORTACION", "portrait",
                        "Reporte de aportaciones obligatorias y voluntarias"),
                plantilla("creditos", "Creditos Desembolsados", "CREDITO", "landscape",
                        "Reporte general de creditos desembolsados"),
                plantilla("usuarios", "Usuarios del Sistema", "USUARIO", "portrait",
                        "Reporte de usuarios registrados en el sistema"),
                plantilla("contrato-credito", "Contrato de Credito", "CREDITO", "portrait",
                        "Plantilla de contrato para creditos desembolsados")
        );

        for (ReportePlantilla p : plantillas) {
            if (reporteRepository.existsByNombre(p.nombre())) {
                continue;
            }
            try {
                ClassPathResource resource = new ClassPathResource("reports/" + p.nombre() + ".jrxml");
                String jrxml = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                Reporte reporte = new Reporte();
                reporte.setNombre(p.nombre());
                reporte.setTitulo(p.titulo());
                reporte.setEntidad(p.entidad());
                reporte.setOrientacion(p.orientacion());
                reporte.setFormatoDefault("pdf");
                reporte.setDescripcion(p.descripcion());
                reporte.setJrxml(jrxml);
                reporte.setActivo(true);
                reporte.setCreatedAt(Instant.now());
                reporteRepository.save(reporte);
            } catch (IOException e) {
                System.err.println("No se pudo cargar plantilla de reporte '" + p.nombre() + "': " + e.getMessage());
            }
        }
    }

    private ReportePlantilla plantilla(String nombre, String titulo, String entidad, String orientacion, String descripcion) {
        return new ReportePlantilla(nombre, titulo, entidad, orientacion, descripcion);
    }

    private record ReportePlantilla(String nombre, String titulo, String entidad, String orientacion, String descripcion) {}
}
