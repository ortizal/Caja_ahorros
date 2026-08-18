package com.alantek.caja.modulo.contabilidad.controller;

import com.alantek.caja.modulo.caja.entity.CajaApertura;
import com.alantek.caja.modulo.caja.repository.CajaAperturaRepository;
import com.alantek.caja.modulo.contabilidad.repository.AsientoContableRepository;
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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CierrePeriodoIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CajaAperturaRepository cajaAperturaRepository;

    @Autowired
    private AsientoContableRepository asientoContableRepository;

    @Test
    void periodoCerradoBloqueaPagoDeGastoYReabrirLoPermite() throws Exception {
        String token = loginToken("admin", "admin123");
        int anio = LocalDate.now().getYear();
        int mes = LocalDate.now().getMonthValue();

        cerrarCajasAbiertas();
        mvc.perform(post("/api/v1/caja/apertura")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"saldoInicial\":500.00}"))
                .andExpect(status().isCreated());

        long cuentaId = idPlanCuenta("5.1.01");
        MvcResult gastoCreado = mvc.perform(post("/api/v1/gastos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concepto\":\"Gasto en periodo cerrado\",\"monto\":80.00,"
                                + "\"cuentaContableId\":" + cuentaId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        long gastoId = objectMapper.readTree(gastoCreado.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/v1/gastos/" + gastoId + "/aprobar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aprobar\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));

        mvc.perform(post("/api/v1/periodos-contables/cerrar?anio=" + anio + "&mes=" + mes)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"));

        mvc.perform(post("/api/v1/gastos/" + gastoId + "/pagar")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("CERRADO")));

        mvc.perform(get("/api/v1/gastos?estado=APROBADO&sort=id,desc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + gastoId + ")].estado").value("APROBADO"));

        mvc.perform(post("/api/v1/periodos-contables/reabrir?anio=" + anio + "&mes=" + mes)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ABIERTO"));

        mvc.perform(post("/api/v1/gastos/" + gastoId + "/pagar")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADO"));
    }

    @Test
    void asientoManualEnPeriodoCerradoEsRechazado() throws Exception {
        String token = loginToken("admin", "admin123");
        int anio = LocalDate.now().getYear();
        int mes = LocalDate.now().getMonthValue();

        mvc.perform(post("/api/v1/periodos-contables/cerrar?anio=" + anio + "&mes=" + mes)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        long cuentaCaja = idPlanCuenta("1.1.01");
        long cuentaCapital = idPlanCuenta("3.1.01");
        long asientosAntes = asientoContableRepository.count();
        mvc.perform(post("/api/v1/asientos-contables")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"" + LocalDate.now() + "\",\"descripcion\":\"Ajuste manual\","
                                + "\"detalles\":[{\"cuentaId\":" + cuentaCaja + ",\"debe\":100.00},"
                                + "{\"cuentaId\":" + cuentaCapital + ",\"haber\":100.00}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("CERRADO")));

        assertThat(asientoContableRepository.count()).isEqualTo(asientosAntes);
    }

    @Test
    void cerrarPeriodoRequiereCONTABILIDADAPROBAR() throws Exception {
        String tokenCajero = loginToken("cajero", "cajero123");
        int anio = LocalDate.now().getYear();
        int mes = LocalDate.now().getMonthValue();

        mvc.perform(post("/api/v1/periodos-contables/cerrar?anio=" + anio + "&mes=" + mes)
                        .header("Authorization", "Bearer " + tokenCajero))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/periodos-contables/reabrir?anio=" + anio + "&mes=" + mes)
                        .header("Authorization", "Bearer " + tokenCajero))
                .andExpect(status().isForbidden());
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
                        .header("Authorization", "Bearer " + token)
                        .param("size", "500"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode cuenta : body.get("content")) {
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
