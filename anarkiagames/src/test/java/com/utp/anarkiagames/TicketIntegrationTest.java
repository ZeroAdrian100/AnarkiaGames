package com.utp.anarkiagames;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tickets con código y check-in contra H2. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private String admin;
    private long torneoId;

    @BeforeEach
    void setUp() throws Exception {
        admin = login("admin@test.com", "admin12345");
        torneoId = idDe(conToken(admin, post("/admin/torneos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Copa QR","juego":"Tekken 8","fechaInicio":"%s"}"""
                        .formatted(LocalDateTime.now().plusDays(10).withNano(0))))
                .andExpect(status().isCreated()));
        conToken(admin, post("/admin/torneos/" + torneoId + "/tipos-ticket")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tipo":"ESPECTADOR","stockMaximo":10,"precio":2000}"""))
                .andExpect(status().isCreated());
    }

    @Test
    void cadaUnidadCompradaTieneSuPropioCodigo() throws Exception {
        final String cliente = registrar();
        final long inscripcionId = comprar(cliente, 3);

        final String respuesta = conToken(cliente, get("/torneos/mis-inscripciones/" + inscripcionId + "/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].codigo").value(matchesPattern("[0-9a-f]{32}")))
                .andExpect(jsonPath("$[0].valido").value(true))
                .andReturn().getResponse().getContentAsString();
        final List<String> codigos = JsonPath.read(respuesta, "$[*].codigo");
        assertEquals(3, new HashSet<>(codigos).size());
    }

    @Test
    void soloElDuenoVeSusTickets() throws Exception {
        final long inscripcionId = comprar(registrar(), 1);

        conToken(registrar(), get("/torneos/mis-inscripciones/" + inscripcionId + "/tickets"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/torneos/mis-inscripciones/" + inscripcionId + "/tickets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkinMarcaElTicketUnaSolaVez() throws Exception {
        final String cliente = registrar();
        final long inscripcionId = comprar(cliente, 1);
        final String codigo = codigos(cliente, inscripcionId).get(0);

        conToken(admin, post("/admin/checkin/" + codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.torneoId").value(torneoId))
                .andExpect(jsonPath("$.tipo").value("ESPECTADOR"));
        conToken(admin, post("/admin/checkin/" + codigo))
                .andExpect(status().isConflict());
        conToken(cliente, get("/torneos/mis-inscripciones/" + inscripcionId + "/tickets"))
                .andExpect(jsonPath("$[0].valido").value(false))
                .andExpect(jsonPath("$[0].fechaUso").isNotEmpty());
    }

    @Test
    void soloAdminHaceCheckinYElCodigoDebeExistir() throws Exception {
        final String cliente = registrar();
        final String codigo = codigos(cliente, comprar(cliente, 1)).get(0);

        conToken(cliente, post("/admin/checkin/" + codigo)).andExpect(status().isForbidden());
        conToken(admin, post("/admin/checkin/no-existe")).andExpect(status().isNotFound());
    }

    @Test
    void reembolsoYCancelacionInvalidanLosTickets() throws Exception {
        final String cliente = registrar();
        final long reembolsada = comprar(cliente, 1);
        final String codigoReembolsado = codigos(cliente, reembolsada).get(0);
        final long otra = comprar(cliente, 1);
        final String codigoCancelado = codigos(cliente, otra).get(0);

        conToken(admin, post("/admin/inscripciones/" + reembolsada + "/reembolsar")).andExpect(status().isOk());
        conToken(admin, post("/admin/checkin/" + codigoReembolsado)).andExpect(status().isConflict());

        conToken(admin, post("/admin/torneos/" + torneoId + "/cancelar")).andExpect(status().isOk());
        conToken(admin, post("/admin/checkin/" + codigoCancelado)).andExpect(status().isConflict());
        conToken(cliente, get("/torneos/mis-inscripciones/" + otra + "/tickets"))
                .andExpect(jsonPath("$[0].valido").value(false));
    }

    // ---------- utilidades ----------

    private ResultActions conToken(final String token,
                                   final org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private long comprar(final String token, final int cantidad) throws Exception {
        return idDe(conToken(token, post("/torneos/" + torneoId + "/inscripciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tipo":"ESPECTADOR","cantidad":%d,"tokenId":"tok_ok"}""".formatted(cantidad)))
                .andExpect(status().isCreated()));
    }

    private List<String> codigos(final String token, final long inscripcionId) throws Exception {
        final String respuesta = conToken(token, get("/torneos/mis-inscripciones/" + inscripcionId + "/tickets"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$[*].codigo");
    }

    private String login(final String email, final String password) throws Exception {
        final String respuesta = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.access_token");
    }

    private String registrar() throws Exception {
        final String respuesta = mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Cliente","email":"cliente-%s@test.com","password":"secreta123"}"""
                                .formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.access_token");
    }

    private static long idDe(final ResultActions resultado) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }
}
