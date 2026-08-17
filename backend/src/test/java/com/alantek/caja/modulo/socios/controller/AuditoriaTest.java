package com.alantek.caja.modulo.socios.controller;

import com.alantek.caja.modulo.seguridad.entity.Auditoria;
import com.alantek.caja.modulo.seguridad.repository.AuditoriaRepository;
import com.alantek.caja.modulo.creditos.repository.CreditoRepository;
import com.alantek.caja.modulo.creditos.repository.SolicitudCreditoRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditoriaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private CreditoRepository creditoRepository;

    @Autowired
    private SolicitudCreditoRepository solicitudRepository;

    @Test
    void crearSocioRegistraAuditoriaConDetalleJson() throws Exception {
        String token = loginToken();

        String body = """
                {"identificacion":"1105999999","nombres":"María","apellidos":"Gómez",
                 "fechaIngreso":"2026-01-10","estado":"ACTIVO",
                 "beneficiarios":[{"nombres":"Luis Gómez","parentesco":"HIJO","porcentaje":100}]}
                """;

        MvcResult result = mvc.perform(post("/api/v1/socios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        Long socioId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        List<Auditoria> registros = auditoriaRepository.filtrar("socios",
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(60));

        assertThat(registros)
                .filteredOn(a -> socioId.equals(a.getRegistroId()) && "CREAR".equals(a.getAccion()))
                .isNotEmpty();

        Auditoria creacion = registros.stream()
                .filter(a -> "CREAR".equals(a.getAccion()) && socioId.equals(a.getRegistroId()))
                .findFirst().orElseThrow();

        assertThat(creacion.getTablaAfectada()).isEqualTo("socios");
        assertThat(creacion.getValorNuevo()).contains("1105999999");
        assertThat(creacion.getUsuarioId()).isNotNull();
        assertThat(creacion.getCreatedAt()).isNotNull();
    }

    @Test
    void aprobarSolicitudCreditoRegistraAuditoriaConUsuario() throws Exception {
        String cajero = loginToken("cajero", "cajero123");
        String admin = loginToken("admin", "admin123");

        Long socioId = primerSocioId(cajero);
        Long productoId = primerProductoId(cajero);

        MvcResult solicitud = mvc.perform(post("/api/v1/solicitudes-credito")
                        .header("Authorization", "Bearer " + cajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"socioId\":" + socioId + ",\"productoId\":" + productoId
                                + ",\"montoSolicitado\":200.00,\"plazoMeses\":6}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long solicitudId = objectMapper.readTree(solicitud.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/solicitudes-credito/" + solicitudId + "/evaluar")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/solicitudes-credito/" + solicitudId + "/aprobar")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aprobar\":true}"))
                .andExpect(status().isOk());

        List<Auditoria> aprobaciones = auditoriaRepository.filtrar("solicitud_credito",
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(60));

        Auditoria aprobacion = aprobaciones.stream()
                .filter(a -> "APROBAR".equals(a.getAccion()) && solicitudId.equals(a.getRegistroId()))
                .findFirst().orElseThrow();
        assertThat(aprobacion.getTablaAfectada()).isEqualTo("solicitud_credito");
        assertThat(aprobacion.getUsuarioId()).isNotNull();
        assertThat(aprobacion.getCreatedAt()).isNotNull();
        assertThat(aprobacion.getValorAnterior()).contains("EVALUACION");
        assertThat(aprobacion.getValorNuevo()).contains("APROBADA");
    }

    private String loginToken() throws Exception {
        return loginToken("admin", "admin123");
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

    private Long primerSocioId(String token) throws Exception {
        MvcResult result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/socios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode socio : body) {
            long socioId = socio.get("id").asLong();
            if (!"ACTIVO".equals(socio.get("estado").asText())) {
                continue;
            }
            if (solicitudRepository.existsBySocioIdAndEstadoIn(socioId,
                    List.of("PENDIENTE", "EVALUACION", "APROBADA"))) {
                continue;
            }
            if (creditoRepository.existsBySocioIdAndEstadoIn(socioId, List.of("VIGENTE", "EN_MORA"))) {
                continue;
            }
            return socioId;
        }
        throw new IllegalStateException("No hay socios ACTIVOS para la prueba");
    }

    private Long primerProductoId(String token) throws Exception {
        MvcResult result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/productos-credito")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get(0).get("id").asLong();
    }
}
