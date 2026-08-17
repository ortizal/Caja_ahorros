package com.alantek.caja.modulo.portal.controller;

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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PortalFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void resumenAhorroAportacionesYCreditosScopedAlSocioAutenticado() throws Exception {
        String socio = loginToken("socio", "socio123");
        String admin = loginToken("admin", "admin123");
        String cajero = loginToken("cajero", "cajero123");

        MvcResult resumen = mvc.perform(get("/api/v1/portal/resumen")
                        .header("Authorization", "Bearer " + socio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.socio.codigo").value("SOC-DEMO-01"))
                .andExpect(jsonPath("$.socio.nombres").value("Maria"))
                .andReturn();
        JsonNode resumenJson = objectMapper.readTree(resumen.getResponse().getContentAsString());
        Long socioId = resumenJson.get("socio").get("id").asLong();

        long productoAhorroId = primerProductoId(admin, "/api/v1/productos-ahorro");
        mvc.perform(post("/api/v1/cuentas-ahorro")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"socioId\":" + socioId + ",\"productoId\":" + productoAhorroId + "}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/portal/ahorro")
                        .header("Authorization", "Bearer " + socio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cuenta.socioId").value(socioId))
                .andExpect(jsonPath("$[0].movimientos").isArray());

        mvc.perform(post("/api/v1/aportaciones/generar?periodo=" + periodoActual())
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/portal/aportaciones")
                        .header("Authorization", "Bearer " + socio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.aportacion.periodo == '" + periodoActual() + "')]").isNotEmpty());

        long socioDemoId = idDeCodigo(admin, "SOC-DEMO-01");
        long productoCreditoId = primerProductoId(admin, "/api/v1/productos-credito/activos");
        MvcResult solicitud = mvc.perform(post("/api/v1/solicitudes-credito")
                        .header("Authorization", "Bearer " + cajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"socioId\":" + socioDemoId + ",\"productoId\":" + productoCreditoId
                                + ",\"montoSolicitado\":1200.00,\"plazoMeses\":12,\"destino\":\"Prueba portal\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long solicitudId = objectMapper.readTree(solicitud.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(put("/api/v1/solicitudes-credito/" + solicitudId + "/evaluar")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        mvc.perform(put("/api/v1/solicitudes-credito/" + solicitudId + "/aprobar")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aprobar\":true}"))
                .andExpect(status().isOk());

        long creditoId = creditoIdDeSolicitud(admin, solicitudId);
        abrirCaja(admin);
        mvc.perform(post("/api/v1/creditos/" + creditoId + "/desembolsar")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VIGENTE"));

        mvc.perform(get("/api/v1/portal/creditos")
                        .header("Authorization", "Bearer " + socio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].credito.estado").value("VIGENTE"))
                .andExpect(jsonPath("$[0].credito.saldoCapital").value(1200.0))
                .andExpect(jsonPath("$[0].cuotas.length()").value(12))
                .andExpect(jsonPath("$[0].pagos").isArray());

        MvcResult resumenFinal = mvc.perform(get("/api/v1/portal/resumen")
                        .header("Authorization", "Bearer " + socio))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode resumenFinalJson = objectMapper.readTree(resumenFinal.getResponse().getContentAsString());
        assertThat(resumenFinalJson.get("saldoCreditoVigente").decimalValue()).isEqualByComparingTo("1200.00");
        assertThat(resumenFinalJson.get("cuotasPendientes").asInt()).isEqualTo(12);
    }

    @Test
    void portalRequierePermisoPORTALVER() throws Exception {
        String cajero = loginToken("cajero", "cajero123");

        mvc.perform(get("/api/v1/portal/resumen")
                        .header("Authorization", "Bearer " + cajero))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/portal/creditos")
                        .header("Authorization", "Bearer " + cajero))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioSinSocioUnicoVinculadoNoPuedeUsarElPortal() throws Exception {
        String admin = loginToken("admin", "admin123");

        mvc.perform(get("/api/v1/portal/resumen")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("vinculado")));
    }

    private long idDeCodigo(String token, String codigo) throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/socios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode lista = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode item : lista) {
            if (codigo.equals(item.get("codigo").asText())) {
                return item.get("id").asLong();
            }
        }
        throw new IllegalStateException("Socio no encontrado: " + codigo);
    }

    private long primerProductoId(String token, String ruta) throws Exception {
        MvcResult result = mvc.perform(get(ruta)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode lista = objectMapper.readTree(result.getResponse().getContentAsString());
        return lista.get(0).get("id").asLong();
    }

    private long creditoIdDeSolicitud(String token, long solicitudId) throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/creditos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode lista = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode credito : lista) {
            if (credito.get("solicitudId").asLong() == solicitudId) {
                return credito.get("id").asLong();
            }
        }
        throw new IllegalStateException("No se creo el credito para la solicitud " + solicitudId);
    }

    private void abrirCaja(String token) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/caja/apertura")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"saldoInicial\":1000.00}"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isIn(201, 400);
    }

    private String periodoActual() {
        LocalDate hoy = LocalDate.now();
        return hoy.getYear() + "-" + (hoy.getMonthValue() < 10 ? "0" : "") + hoy.getMonthValue();
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
