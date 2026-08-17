package com.alantek.caja.modulo.creditos.controller;

import com.alantek.caja.modulo.creditos.entity.CuotaCredito;
import com.alantek.caja.modulo.creditos.entity.ProductoCredito;
import com.alantek.caja.modulo.creditos.repository.CreditoRepository;
import com.alantek.caja.modulo.creditos.repository.CuotaCreditoRepository;
import com.alantek.caja.modulo.creditos.repository.ProductoCreditoRepository;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

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
class CreditosFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CuotaCreditoRepository cuotaRepository;

    @Autowired
    private ProductoCreditoRepository productoRepository;

    @Autowired
    private CreditoRepository creditoRepository;

    @Autowired
    private SolicitudCreditoRepository solicitudRepository;

    @Test
    void cicloCompletoSolicitudDesembolsoYPagoDeCuota() throws Exception {
        String cajero = loginToken("cajero", "cajero123");
        String admin = loginToken("admin", "admin123");

        MvcResult producto = mvc.perform(post("/api/v1/productos-credito")
                        .header("Authorization", "Bearer " + cajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"PERSONAL E2E\",\"tasaInteres\":18.0000,"
                                + "\"tasaMora\":1.0000,\"sistemaAmortizacion\":\"FRANCES\","
                                + "\"plazoMaxMeses\":24,\"montoMin\":50.00,\"montoMax\":5000.00,"
                                + "\"requiereGarante\":false,\"vigenteDesde\":\"2026-01-01\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long productoId = objectMapper.readTree(producto.getResponse().getContentAsString()).get("id").asLong();

        Long socioId = primerSocioId(cajero);
        MvcResult solicitud = mvc.perform(post("/api/v1/solicitudes-credito")
                        .header("Authorization", "Bearer " + cajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"socioId\":" + socioId + ",\"productoId\":" + productoId
                                + ",\"montoSolicitado\":1200.00,\"plazoMeses\":12,"
                                + "\"destino\":\"Compra de enseres\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn();
        Long solicitudId = objectMapper.readTree(solicitud.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(put("/api/v1/solicitudes-credito/" + solicitudId + "/evaluar")
                        .header("Authorization", "Bearer " + cajero))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/solicitudes-credito/" + solicitudId + "/evaluar")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EVALUACION"))
                .andExpect(jsonPath("$.evaluadoPor").isNumber());

        mvc.perform(put("/api/v1/solicitudes-credito/" + solicitudId + "/aprobar")
                        .header("Authorization", "Bearer " + cajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aprobar\":true}"))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/solicitudes-credito/" + solicitudId + "/aprobar")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aprobar\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.aprobadoPor").isNumber());

        Long creditoId = creditoIdDeSolicitud(admin, solicitudId);
        abrirCaja(admin);

        mvc.perform(post("/api/v1/creditos/" + creditoId + "/desembolsar")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VIGENTE"))
                .andExpect(jsonPath("$.saldoCapital").value(1200.0));

        MvcResult cuotas = mvc.perform(get("/api/v1/creditos/" + creditoId + "/amortizacion")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode tabla = objectMapper.readTree(cuotas.getResponse().getContentAsString());
        assertThat(tabla).hasSize(12);
        assertThat(tabla.get(0).get("estado").asText()).isEqualTo("PENDIENTE");
        assertThat(tabla.get(0).get("cuotaTotal").decimalValue()).isGreaterThan(BigDecimal.ZERO);
        Long cuotaId = tabla.get(0).get("id").asLong();

        abrirCaja(cajero);
        mvc.perform(post("/api/v1/creditos/" + creditoId + "/pagos")
                        .header("Authorization", "Bearer " + cajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cuotaId\":" + cuotaId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comprobanteNumero").isNotEmpty())
                .andExpect(jsonPath("$.montoCapital").isNumber());

        MvcResult creditoResult = mvc.perform(get("/api/v1/creditos/" + creditoId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode creditoJson = objectMapper.readTree(creditoResult.getResponse().getContentAsString());
        assertThat(creditoJson.get("saldoCapital").decimalValue())
                .isLessThan(new BigDecimal("1200.00"));
        assertThat(creditoJson.get("cuotasPendientes").asInt()).isEqualTo(11);

        mvc.perform(get("/api/v1/creditos/" + creditoId + "/amortizacion")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("PAGADA"));

        mvc.perform(get("/api/v1/creditos/" + creditoId + "/pagos")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rechazoDeSolicitudRequiereMotivoYValidaPermisos() throws Exception {
        String cajero = loginToken("cajero", "cajero123");
        String admin = loginToken("admin", "admin123");
        String contador = loginToken("contador", "contador123");

        Long socioId = primerSocioId(cajero);
        Long productoId = primerProductoId(cajero);

        mvc.perform(post("/api/v1/solicitudes-credito")
                        .header("Authorization", "Bearer " + contador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"socioId\":" + socioId + ",\"productoId\":" + productoId
                                + ",\"montoSolicitado\":200.00,\"plazoMeses\":6}"))
                .andExpect(status().isForbidden());

        MvcResult solicitud = mvc.perform(post("/api/v1/solicitudes-credito")
                        .header("Authorization", "Bearer " + cajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"socioId\":" + socioId + ",\"productoId\":" + productoId
                                + ",\"montoSolicitado\":200.00,\"plazoMeses\":6}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long solicitudId = objectMapper.readTree(solicitud.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(put("/api/v1/solicitudes-credito/" + solicitudId + "/evaluar")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        mvc.perform(put("/api/v1/solicitudes-credito/" + solicitudId + "/aprobar")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aprobar\":false}"))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/v1/solicitudes-credito/" + solicitudId + "/aprobar")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aprobar\":false,\"motivoRechazo\":\"Bajo perfil de ingresos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivoRechazo").value("Bajo perfil de ingresos"));
    }

    @Test
    void moraYRefinanciamiento() throws Exception {
        String cajero = loginToken("cajero", "cajero123");
        String admin = loginToken("admin", "admin123");

        Long socioId = primerSocioId(cajero);
        Long productoId = primerProductoId(cajero);

        MvcResult solicitud = mvc.perform(post("/api/v1/solicitudes-credito")
                        .header("Authorization", "Bearer " + cajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"socioId\":" + socioId + ",\"productoId\":" + productoId
                                + ",\"montoSolicitado\":600.00,\"plazoMeses\":6}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long solicitudId = objectMapper.readTree(solicitud.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(put("/api/v1/solicitudes-credito/" + solicitudId + "/evaluar")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/solicitudes-credito/" + solicitudId + "/aprobar")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aprobar\":true}"))
                .andExpect(status().isOk());

        Long creditoId = creditoIdDeSolicitud(admin, solicitudId);
        abrirCaja(admin);
        mvc.perform(post("/api/v1/creditos/" + creditoId + "/desembolsar")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        List<CuotaCredito> cuotas = cuotaRepository.findByCreditoIdOrderByNumeroCuotaAsc(creditoId);
        CuotaCredito primera = cuotas.get(0);
        primera.setFechaVencimiento(LocalDate.now().minusDays(30));
        cuotaRepository.save(primera);

        MvcResult moraResult = mvc.perform(post("/api/v1/creditos/procesar-vencidas")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode mora = objectMapper.readTree(moraResult.getResponse().getContentAsString());
        assertThat(mora.get("cuotasMarcadas").asInt()).isEqualTo(1);
        assertThat(mora.get("moraTotal").decimalValue()).isGreaterThan(BigDecimal.ZERO);
        assertThat(mora.get("creditosEnMora").asInt()).isEqualTo(1);

        mvc.perform(get("/api/v1/creditos/" + creditoId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_MORA"));

        mvc.perform(post("/api/v1/creditos/" + creditoId + "/refinanciar")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plazoMeses\":24,\"tasaInteres\":15.0000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VIGENTE"))
                .andExpect(jsonPath("$.plazoMeses").value(24));

        mvc.perform(get("/api/v1/creditos/" + creditoId + "/amortizacion")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(24));
    }

    @Test
    void moraUsaFormulaDeLaSeccion5ConValoresExactos() throws Exception {
        String cajero = loginToken("cajero", "cajero123");
        String admin = loginToken("admin", "admin123");

        Long socioId = primerSocioId(cajero);
        MvcResult producto = mvc.perform(post("/api/v1/productos-credito")
                        .header("Authorization", "Bearer " + cajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"PERSONAL MORA\",\"tasaInteres\":18.0000,"
                                + "\"tasaMora\":1.0000,\"sistemaAmortizacion\":\"FRANCES\","
                                + "\"plazoMaxMeses\":12,\"montoMin\":50.00,\"montoMax\":5000.00,"
                                + "\"requiereGarante\":false,\"vigenteDesde\":\"2026-01-01\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long productoId = objectMapper.readTree(producto.getResponse().getContentAsString()).get("id").asLong();

        MvcResult solicitud = mvc.perform(post("/api/v1/solicitudes-credito")
                        .header("Authorization", "Bearer " + cajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"socioId\":" + socioId + ",\"productoId\":" + productoId
                                + ",\"montoSolicitado\":1200.00,\"plazoMeses\":12,"
                                + "\"destino\":\"Mora exacta\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long solicitudId = objectMapper.readTree(solicitud.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(put("/api/v1/solicitudes-credito/" + solicitudId + "/evaluar")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/solicitudes-credito/" + solicitudId + "/aprobar")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aprobar\":true}"))
                .andExpect(status().isOk());

        Long creditoId = creditoIdDeSolicitud(admin, solicitudId);
        abrirCaja(admin);
        mvc.perform(post("/api/v1/creditos/" + creditoId + "/desembolsar")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        CuotaCredito primera = cuotaRepository.findByCreditoIdOrderByNumeroCuotaAsc(creditoId).get(0);
        assertThat(primera.getCapital()).isCloseTo(new BigDecimal("92.02"),
                org.assertj.core.data.Offset.offset(new BigDecimal("0.01")));

        primera.setFechaVencimiento(LocalDate.now().minusDays(30));
        cuotaRepository.save(primera);

        BigDecimal esperado = primera.getCapital()
                .multiply(new BigDecimal("1.00")).divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("30"))
                .divide(new BigDecimal("360"), 2, RoundingMode.HALF_UP);

        MvcResult moraResult = mvc.perform(post("/api/v1/creditos/procesar-vencidas")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode mora = objectMapper.readTree(moraResult.getResponse().getContentAsString());
        assertThat(mora.get("cuotasMarcadas").asInt()).isEqualTo(1);
        assertThat(mora.get("moraTotal").decimalValue())
                .isEqualByComparingTo(esperado);
        assertThat(mora.get("creditosEnMora").asInt()).isEqualTo(1);

        CuotaCredito actualizada = cuotaRepository.findByCreditoIdOrderByNumeroCuotaAsc(creditoId).get(0);
        assertThat(actualizada.getEstado()).isEqualTo("VENCIDA");
        assertThat(actualizada.getMora()).isEqualByComparingTo(esperado);
    }

    @Test
    void cambioDeTasaVigenteNoAlteraCuotasYaGeneradas() throws Exception {
        String cajero = loginToken("cajero", "cajero123");
        String admin = loginToken("admin", "admin123");

        Long socioId = primerSocioId(cajero);
        MvcResult producto = mvc.perform(post("/api/v1/productos-credito")
                        .header("Authorization", "Bearer " + cajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"PERSONAL HISTORICO\",\"tasaInteres\":18.0000,"
                                + "\"tasaMora\":1.0000,\"sistemaAmortizacion\":\"FRANCES\","
                                + "\"plazoMaxMeses\":12,\"montoMin\":50.00,\"montoMax\":5000.00,"
                                + "\"requiereGarante\":false,\"vigenteDesde\":\"2026-01-01\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long productoId = objectMapper.readTree(producto.getResponse().getContentAsString()).get("id").asLong();

        MvcResult solicitud = mvc.perform(post("/api/v1/solicitudes-credito")
                        .header("Authorization", "Bearer " + cajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"socioId\":" + socioId + ",\"productoId\":" + productoId
                                + ",\"montoSolicitado\":1200.00,\"plazoMeses\":12,"
                                + "\"destino\":\"Historico de tasas\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long solicitudId = objectMapper.readTree(solicitud.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(put("/api/v1/solicitudes-credito/" + solicitudId + "/evaluar")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/solicitudes-credito/" + solicitudId + "/aprobar")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aprobar\":true}"))
                .andExpect(status().isOk());

        Long creditoId = creditoIdDeSolicitud(admin, solicitudId);
        abrirCaja(admin);
        mvc.perform(post("/api/v1/creditos/" + creditoId + "/desembolsar")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasaInteres").value(18.0));

        MvcResult cuotasAntes = mvc.perform(get("/api/v1/creditos/" + creditoId + "/amortizacion")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode tablaAntes = objectMapper.readTree(cuotasAntes.getResponse().getContentAsString());
        BigDecimal capitalAntes = tablaAntes.get(0).get("capital").decimalValue();
        BigDecimal interesAntes = tablaAntes.get(0).get("interes").decimalValue();
        BigDecimal cuotaAntes = tablaAntes.get(0).get("cuotaTotal").decimalValue();

        ProductoCredito productoActualizado = productoRepository.findById(productoId).orElseThrow();
        productoActualizado.setTasaInteres(new BigDecimal("22.00"));
        productoRepository.save(productoActualizado);

        mvc.perform(get("/api/v1/creditos/" + creditoId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasaInteres").value(18.0));

        MvcResult cuotasDespues = mvc.perform(get("/api/v1/creditos/" + creditoId + "/amortizacion")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode tablaDespues = objectMapper.readTree(cuotasDespues.getResponse().getContentAsString());
        assertThat(tablaDespues).hasSize(12);
        assertThat(tablaDespues.get(0).get("capital").decimalValue()).isEqualByComparingTo(capitalAntes);
        assertThat(tablaDespues.get(0).get("interes").decimalValue()).isEqualByComparingTo(interesAntes);
        assertThat(tablaDespues.get(0).get("cuotaTotal").decimalValue()).isEqualByComparingTo(cuotaAntes);
    }

    @Test
    void simuladorReutilizaCalculoPuro() throws Exception {
        String cajero = loginToken("cajero", "cajero123");

        MvcResult resultado = mvc.perform(post("/api/v1/simulador-credito")
                        .header("Authorization", "Bearer " + cajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":1000.00,\"plazoMeses\":12,"
                                + "\"tasaInteres\":18.0000,\"sistemaAmortizacion\":\"FRANCES\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(resultado.getResponse().getContentAsString());
        assertThat(body.get("cuotas")).hasSize(12);
        assertThat(body.get("cuotaMensual").decimalValue())
                .isCloseTo(new BigDecimal("91.68"), org.assertj.core.data.Offset.offset(new BigDecimal("0.01")));
        assertThat(body.get("totalPagar").decimalValue())
                .isGreaterThan(new BigDecimal("1000.00"));
    }

    @Test
    void solicitanteNoPuedeEvaluarNiAprobarSuPropiaSolicitud() throws Exception {
        String analista = loginToken("credito", "credito123");

        Long socioId = primerSocioId(analista);
        Long productoId = primerProductoId(analista);

        MvcResult solicitud = mvc.perform(post("/api/v1/solicitudes-credito")
                        .header("Authorization", "Bearer " + analista)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"socioId\":" + socioId + ",\"productoId\":" + productoId
                                + ",\"montoSolicitado\":150.00,\"plazoMeses\":6}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long solicitudId = objectMapper.readTree(solicitud.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(put("/api/v1/solicitudes-credito/" + solicitudId + "/evaluar")
                        .header("Authorization", "Bearer " + analista))
                .andExpect(status().isBadRequest());
    }

    @Test
    void solicitudRechazadaSiMontoSuperaMaximoDelProducto() throws Exception {
        String cajero = loginToken("cajero", "cajero123");
        Long socioId = primerSocioId(cajero);
        Long productoId = primerProductoId(cajero);

        mvc.perform(post("/api/v1/solicitudes-credito")
                        .header("Authorization", "Bearer " + cajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"socioId\":" + socioId + ",\"productoId\":" + productoId
                                + ",\"montoSolicitado\":99999.00,\"plazoMeses\":12}"))
                .andExpect(status().isBadRequest());
    }

    private Long creditoIdDeSolicitud(String token, Long solicitudId) throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/creditos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode credito : body) {
            if (credito.get("solicitudId").asLong() == solicitudId) {
                return credito.get("id").asLong();
            }
        }
        throw new IllegalStateException("No se creo el credito para la solicitud " + solicitudId);
    }

    private Long primerProductoId(String token) throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/productos-credito")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get(0).get("id").asLong();
    }

    private Long primerSocioId(String token) throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/socios")
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

    private void abrirCaja(String token) throws Exception {
        mvc.perform(post("/api/v1/caja/apertura")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"saldoInicial\":1000.00}"));
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
