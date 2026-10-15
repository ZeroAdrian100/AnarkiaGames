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
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Panel de administración contra H2: torneos, tipos de ticket, resumen, reembolsos y usuarios. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private String admin;
    private long torneoId;
    private long espectadorId;
    private long competidorId;

    @BeforeEach
    void setUp() throws Exception {
        admin = login("admin@test.com", "admin12345");
        torneoId = crearTorneo();
        espectadorId = crearTipoTicket("ESPECTADOR", 10, 2000);
        competidorId = crearTipoTicket("COMPETIDOR", 2, 5000);
    }

    @Test
    void resumenMuestraStockEIngresos() throws Exception {
        comprar(registrar(), "ESPECTADOR", 3).andExpect(status().isCreated());
        comprar(registrar(), "COMPETIDOR", 1).andExpect(status().isCreated());

        conAdmin(get("/admin/torneos/" + torneoId + "/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tiposTicket[?(@.tipo == 'ESPECTADOR')].vendidos").value(contains(3)))
                .andExpect(jsonPath("$.tiposTicket[?(@.tipo == 'ESPECTADOR')].disponibles").value(contains(7)))
                .andExpect(jsonPath("$.tiposTicket[?(@.tipo == 'ESPECTADOR')].ingresos").value(contains(6000)))
                .andExpect(jsonPath("$.tiposTicket[?(@.tipo == 'COMPETIDOR')].ingresos").value(contains(5000)))
                .andExpect(jsonPath("$.ingresosTotales").value(11000));
    }

    @Test
    void cancelarTorneoReembolsaTodoYCierraVentas() throws Exception {
        final String cliente = registrar();
        comprar(cliente, "ESPECTADOR", 2).andExpect(status().isCreated());
        comprar(registrar(), "COMPETIDOR", 1).andExpect(status().isCreated());

        conAdmin(post("/admin/torneos/" + torneoId + "/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reembolsadas").value(2))
                .andExpect(jsonPath("$.pendientes").value(0));

        mvc.perform(get("/torneos/" + torneoId)).andExpect(jsonPath("$.estado").value("CANCELADO"));
        comprar(registrar(), "ESPECTADOR", 1).andExpect(status().isConflict());
        conAdmin(get("/admin/torneos/" + torneoId + "/resumen"))
                .andExpect(jsonPath("$.ingresosTotales").value(0));
        conAdmin(get("/admin/torneos/" + torneoId + "/inscripciones"))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.contenido[*].estado").value(everyItem(is("REEMBOLSADA"))))
                .andExpect(jsonPath("$.contenido[*].referenciaReembolso").value(everyItem(notNullValue())));
        mvc.perform(get("/torneos/mis-inscripciones").header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(jsonPath("$[0].estado").value("REEMBOLSADA"));

        // Cancelar de nuevo no vuelve a reembolsar nada.
        conAdmin(post("/admin/torneos/" + torneoId + "/cancelar"))
                .andExpect(jsonPath("$.reembolsadas").value(0))
                .andExpect(jsonPath("$.pendientes").value(0));
        conAdmin(put("/admin/torneos/" + torneoId).contentType(MediaType.APPLICATION_JSON).content(torneoJson("Editado")))
                .andExpect(status().isConflict());
    }

    @Test
    void reembolsoIndividualLiberaCupoYPermiteOtraParticipacion() throws Exception {
        final String cliente = registrar();
        final long inscripcionId = idDe(comprar(cliente, "COMPETIDOR", 1).andExpect(status().isCreated()));

        conAdmin(post("/admin/inscripciones/" + inscripcionId + "/reembolsar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("REEMBOLSADA"))
                .andExpect(jsonPath("$.referenciaReembolso").value(notNullValue()));
        conAdmin(post("/admin/inscripciones/" + inscripcionId + "/reembolsar"))
                .andExpect(status().isConflict());

        mvc.perform(get("/torneos/" + torneoId))
                .andExpect(jsonPath("$.tiposTicket[?(@.tipo == 'COMPETIDOR')].disponibles").value(contains(2)));
        comprar(cliente, "ESPECTADOR", 1).andExpect(status().isCreated());
    }

    @Test
    void actualizarTipoTicketNoBajaDeLoVendido() throws Exception {
        comprar(registrar(), "ESPECTADOR", 3).andExpect(status().isCreated());

        conAdmin(put("/admin/torneos/" + torneoId + "/tipos-ticket/" + espectadorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"stockMaximo":2,"precio":2000}"""))
                .andExpect(status().isConflict());
        conAdmin(put("/admin/torneos/" + torneoId + "/tipos-ticket/" + espectadorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"stockMaximo":20,"precio":2500,"descripcion":"General"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibles").value(17))
                .andExpect(jsonPath("$.precio").value(2500));
    }

    @Test
    void eliminarTipoTicketSoloSinVentas() throws Exception {
        comprar(registrar(), "ESPECTADOR", 1).andExpect(status().isCreated());

        conAdmin(delete("/admin/torneos/" + torneoId + "/tipos-ticket/" + espectadorId))
                .andExpect(status().isConflict());
        conAdmin(delete("/admin/torneos/" + (torneoId + 1000) + "/tipos-ticket/" + competidorId))
                .andExpect(status().isNotFound());
        conAdmin(delete("/admin/torneos/" + torneoId + "/tipos-ticket/" + competidorId))
                .andExpect(status().isNoContent());
    }

    @Test
    void actualizarTorneoValidaLaFecha() throws Exception {
        conAdmin(put("/admin/torneos/" + torneoId).contentType(MediaType.APPLICATION_JSON).content(torneoJson("Copa editada")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Copa editada"));
        conAdmin(put("/admin/torneos/" + torneoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"X","juego":"Y","fechaInicio":"2020-01-01T10:00:00"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buscarUsuariosYCambiarRol() throws Exception {
        final String email = emailUnico();
        final String cliente = registrar(email);

        final String pagina = conAdmin(get("/admin/usuarios").param("buscar", email.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andReturn().getResponse().getContentAsString();
        final long usuarioId = ((Number) JsonPath.read(pagina, "$.contenido[0].id")).longValue();

        conAdmin(patch("/admin/usuarios/" + usuarioId + "/rol")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"role":"ADMIN"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(get("/admin/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(status().isOk());

        conAdmin(get("/admin/usuarios").param("size", "1"))
                .andExpect(jsonPath("$.tamano").value(1));
        conAdmin(get("/admin/usuarios").param("sort", "noExiste"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminNoPuedeCambiarSuPropioRol() throws Exception {
        final String pagina = conAdmin(get("/admin/usuarios").param("buscar", "admin@test.com"))
                .andReturn().getResponse().getContentAsString();
        final long adminId = ((Number) JsonPath.read(pagina, "$.contenido[0].id")).longValue();

        conAdmin(patch("/admin/usuarios/" + adminId + "/rol")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"role":"CLIENTE"}"""))
                .andExpect(status().isConflict());
    }

    // ---------- utilidades ----------

    private ResultActions conAdmin(final org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + admin));
    }

    private ResultActions comprar(final String token, final String tipo, final int cantidad) throws Exception {
        return mvc.perform(post("/torneos/" + torneoId + "/inscripciones")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tipo":"%s","cantidad":%d,"tokenId":"tok_ok","gamerTag":"%s"}"""
                        .formatted(tipo, cantidad, UUID.randomUUID())));
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
        return registrar(emailUnico());
    }

    private String registrar(final String email) throws Exception {
        final String respuesta = mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Cliente","email":"%s","password":"secreta123"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.access_token");
    }

    private long crearTorneo() throws Exception {
        return idDe(conAdmin(post("/admin/torneos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(torneoJson("Copa admin")))
                .andExpect(status().isCreated()));
    }

    private long crearTipoTicket(final String tipo, final int stock, final long precio) throws Exception {
        return idDe(conAdmin(post("/admin/torneos/" + torneoId + "/tipos-ticket")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tipo":"%s","stockMaximo":%d,"precio":%d}""".formatted(tipo, stock, precio)))
                .andExpect(status().isCreated()));
    }

    private static long idDe(final ResultActions resultado) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private static String torneoJson(final String nombre) {
        return """
                {"nombre":"%s","juego":"Tekken 8","fechaInicio":"%s"}"""
                .formatted(nombre, LocalDateTime.now().plusDays(10).withNano(0));
    }

    private static String emailUnico() {
        return "cliente-" + UUID.randomUUID() + "@test.com";
    }
}
