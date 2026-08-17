package com.alantek.caja.modulo.tesoreria.controller;

import com.alantek.caja.modulo.caja.entity.CajaApertura;
import com.alantek.caja.modulo.caja.repository.CajaAperturaRepository;
import com.alantek.caja.modulo.contabilidad.entity.AsientoContable;
import com.alantek.caja.modulo.contabilidad.entity.AsientoDetalle;
import com.alantek.caja.modulo.contabilidad.repository.AsientoContableRepository;
import com.alantek.caja.modulo.contabilidad.repository.AsientoDetalleRepository;
import com.alantek.caja.modulo.seguridad.entity.Auditoria;
import com.alantek.caja.modulo.seguridad.repository.AuditoriaRepository;
import com.alantek.caja.modulo.tesoreria.entity.Gasto;
import com.alantek.caja.modulo.tesoreria.repository.GastoRepository;
import com.alantek.caja.modulo.tesoreria.repository.PresupuestoPartidaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TesoreriaFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CajaAperturaRepository cajaAperturaRepository;

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private AsientoContableRepository asientoContableRepository;

    @Autowired
    private AsientoDetalleRepository asientoDetalleRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private PresupuestoPartidaRepository presupuestoPartidaRepository;

    @Test
    void flujoCompletoDeGastosConAprobacionYAsiento() throws Exception {
        String token = loginToken("admin", "admin123");
        cerrarCajasAbiertas();
        mvc.perform(post("/api/v1/caja/apertura")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"saldoInicial\":1000.00}"))
                .andExpect(status().isCreated());

        long cuentaId = idPlanCuenta("5.1.01");

        MvcResult creado = mvc.perform(post("/api/v1/gastos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concepto\":\"Servicios basicos\",\"descripcion\":\"Luz y agua\","
                                + "\"monto\":150.00,\"cuentaContableId\":" + cuentaId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.solicitadoPor").isNotEmpty())
                .andReturn();
        long gastoId = objectMapper.readTree(creado.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/v1/gastos?estado=PENDIENTE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(gastoId));

        mvc.perform(post("/api/v1/gastos/" + gastoId + "/aprobar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aprobar\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"))
                .andExpect(jsonPath("$.aprobadoPor").isNotEmpty());

        MvcResult pagado = mvc.perform(post("/api/v1/gastos/" + gastoId + "/pagar")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADO"))
                .andReturn();
        JsonNode pagadoBody = objectMapper.readTree(pagado.getResponse().getContentAsString());
        long comprobanteId = pagadoBody.get("comprobanteId").asLong();
        assertThat(pagadoBody.get("cajaMovimientoId").asLong()).isPositive();

        Gasto gasto = gastoRepository.findById(gastoId).orElseThrow();
        assertThat(gasto.getEstado()).isEqualTo("PAGADO");
        assertThat(gasto.getCajaMovimientoId()).isNotNull();

        AsientoContable asiento = asientoContableRepository.findFirstByComprobanteIdOrderByIdAsc(comprobanteId)
                .orElseThrow();
        List<AsientoDetalle> detalles = asientoDetalleRepository.findByAsiento_Id(asiento.getId());
        assertThat(detalles).hasSize(2);
        long cuentaGastos = idPlanCuenta("5.1.01");
        long cuentaCaja = idPlanCuenta("1.1.01");
        AsientoDetalle debeGasto = detalles.stream().filter(d -> d.getCuentaId().equals(cuentaGastos)).findFirst().orElseThrow();
        AsientoDetalle haberCaja = detalles.stream().filter(d -> d.getCuentaId().equals(cuentaCaja)).findFirst().orElseThrow();
        assertThat(debeGasto.getDebe()).isEqualByComparingTo("150.00");
        assertThat(haberCaja.getHaber()).isEqualByComparingTo("150.00");

        List<Auditoria> auditoriaGasto = auditoriaRepository.filtrar("gasto",
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(60));
        assertThat(auditoriaGasto).anySatisfy(a -> {
            assertThat(a.getRegistroId()).isEqualTo(gastoId);
            assertThat(a.getAccion()).isEqualTo("CREAR");
            assertThat(a.getUsuarioId()).isNotNull();
        });
        assertThat(auditoriaGasto).anySatisfy(a -> {
            assertThat(a.getAccion()).isEqualTo("EDITAR");
            assertThat(a.getRegistroId()).isEqualTo(gastoId);
        });
    }

    @Test
    void gastoPuedeSerRechazado() throws Exception {
        String token = loginToken("admin", "admin123");
        long cuentaId = idPlanCuenta("5.1.01");

        MvcResult creado = mvc.perform(post("/api/v1/gastos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concepto\":\"Compra no aprobada\",\"monto\":90.00,\"cuentaContableId\":" + cuentaId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        long gastoId = objectMapper.readTree(creado.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/v1/gastos/" + gastoId + "/aprobar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aprobar\":false,\"motivoRechazo\":\"No presupuestado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.motivoRechazo").value("No presupuestado"));
    }

    @Test
    void cuentasPorPagarYPorCobrarSePaganYCobranConSuAsiento() throws Exception {
        String token = loginToken("admin", "admin123");
        cerrarCajasAbiertas();
        mvc.perform(post("/api/v1/caja/apertura")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"saldoInicial\":0.00}"))
                .andExpect(status().isCreated());

        long cuentaCxp = idPlanCuenta("2.2.01");
        MvcResult cxp = mvc.perform(post("/api/v1/cuentas-por-pagar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proveedor\":\"Proveedor X\",\"concepto\":\"Materiales\","
                                + "\"monto\":200.00,\"cuentaContableId\":" + cuentaCxp
                                + ",\"fechaVencimiento\":\"2026-09-15\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn();
        long cxpId = objectMapper.readTree(cxp.getResponse().getContentAsString()).get("id").asLong();

        MvcResult cxpPagada = mvc.perform(post("/api/v1/cuentas-por-pagar/" + cxpId + "/pagar")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADA"))
                .andReturn();
        long cxpComprobante = objectMapper.readTree(cxpPagada.getResponse().getContentAsString()).get("comprobanteId").asLong();

        AsientoContable asientoCxp = asientoContableRepository.findFirstByComprobanteIdOrderByIdAsc(cxpComprobante).orElseThrow();
        assertThat(asientoDetalleRepository.findByAsiento_Id(asientoCxp.getId())).hasSize(2);

        long cuentaCxc = idPlanCuenta("1.4.01");
        MvcResult cxc = mvc.perform(post("/api/v1/cuentas-por-cobrar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deudor\":\"Deudor Y\",\"concepto\":\"Recuperacion\","
                                + "\"monto\":300.00,\"cuentaContableId\":" + cuentaCxc
                                + ",\"fechaVencimiento\":\"2026-10-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn();
        long cxcId = objectMapper.readTree(cxc.getResponse().getContentAsString()).get("id").asLong();

        MvcResult cxcCobrada = mvc.perform(post("/api/v1/cuentas-por-cobrar/" + cxcId + "/cobrar")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COBRADA"))
                .andReturn();
        long cxcComprobante = objectMapper.readTree(cxcCobrada.getResponse().getContentAsString()).get("comprobanteId").asLong();

        AsientoContable asientoCxc = asientoContableRepository.findFirstByComprobanteIdOrderByIdAsc(cxcComprobante).orElseThrow();
        List<AsientoDetalle> detallesCxc = asientoDetalleRepository.findByAsiento_Id(asientoCxc.getId());
        assertThat(detallesCxc).hasSize(2);
        long cuentaCaja = idPlanCuenta("1.1.01");
        AsientoDetalle debeCaja = detallesCxc.stream()
                .filter(d -> d.getCuentaId().equals(cuentaCaja)).findFirst().orElseThrow();
        AsientoDetalle haberCxc = detallesCxc.stream()
                .filter(d -> d.getCuentaId().equals(cuentaCxc)).findFirst().orElseThrow();
        assertThat(debeCaja.getDebe()).isEqualByComparingTo("300.00");
        assertThat(haberCxc.getHaber()).isEqualByComparingTo("300.00");
    }

    @Test
    void presupuestoCalculaEjecutadoPorCuenta() throws Exception {
        String token = loginToken("admin", "admin123");
        int anio = 2026;
        long cuentaId = idPlanCuenta("5.1.01");
        presupuestoPartidaRepository.deleteAll(presupuestoPartidaRepository.findByAnioOrderByConceptoAsc(anio));

        MvcResult partida = mvc.perform(post("/api/v1/presupuesto/partidas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"anio\":" + anio + ",\"concepto\":\"Gastos administrativos\","
                                + "\"cuentaContableId\":" + cuentaId + ",\"montoPresupuestado\":1000.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montoPresupuestado").value(1000.0))
                .andReturn();
        long partidaId = objectMapper.readTree(partida.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/v1/presupuesto/partidas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"anio\":" + anio + ",\"concepto\":\"Gastos administrativos\","
                                + "\"cuentaContableId\":" + cuentaId + ",\"montoPresupuestado\":500.00}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/presupuesto?anio=" + anio)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anio").value(anio))
                .andExpect(jsonPath("$.partidas[0].id").value(partidaId))
                .andExpect(jsonPath("$.totalPresupuestado").value(1000.0));
    }

    @Test
    void separacionDeFuncionesYPermisos() throws Exception {
        String tokenCajero = loginToken("cajero", "cajero123");
        String tokenCredito = loginToken("credito", "credito123");

        long cuentaId = idPlanCuenta("5.1.01");
        MvcResult creado = mvc.perform(post("/api/v1/gastos")
                        .header("Authorization", "Bearer " + tokenCajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concepto\":\"Gasto del cajero\",\"monto\":25.00,\"cuentaContableId\":" + cuentaId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        long gastoId = objectMapper.readTree(creado.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/v1/gastos/" + gastoId + "/aprobar")
                        .header("Authorization", "Bearer " + tokenCajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aprobar\":true}"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/gastos")
                        .header("Authorization", "Bearer " + tokenCredito))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/gastos")
                        .header("Authorization", "Bearer " + tokenCajero))
                .andExpect(status().isOk());
    }

    private void cerrarCajasAbiertas() {
        for (CajaApertura apertura : cajaAperturaRepository.findByEstadoOrderByOpenedAtAsc("ABIERTA")) {
            apertura.setEstado("CERRADA");
            apertura.setClosedAt(Instant.now());
            cajaAperturaRepository.save(apertura);
        }
    }

    private long idPlanCuenta(String codigo) throws Exception {
        String token = loginToken("admin", "admin123");
        MvcResult result = mvc.perform(get("/api/v1/plan-cuentas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode cuentas = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode cuenta : cuentas) {
            if (codigo.equals(cuenta.get("codigo").asText())) {
                return cuenta.get("id").asLong();
            }
        }
        throw new IllegalStateException("Cuenta no encontrada: " + codigo);
    }

    private String loginToken(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("token").asText();
    }
}
