package cl.duoc.cloud.ordenes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import cl.duoc.cloud.ordenes.mensajeria.PublicadorEventos;
import cl.duoc.cloud.ordenes.mensajeria.evento.OrdenCreadaEvento;
import cl.duoc.cloud.ordenes.service.CatalogoClient;

/**
 * Pruebas del controlador de ordenes.
 *
 * El publicador se sustituye por un doble: estas pruebas comprueban la API y la
 * autorizacion, no la mensajeria, y no deben exigir un broker levantado. Que el
 * evento se publique de verdad se verifica con el cluster real mediante
 * scripts/verificar-topologia-rabbit.sh.
 */
@SpringBootTest
class OrdenV1ControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    @MockitoBean
    private CatalogoClient catalogo;

    @MockitoBean
    private PublicadorEventos publicador;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    void elEndpointPublicoNoExigeToken() throws Exception {
        mockMvc.perform(get("/api/v1/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicio").value("ms-ordenes"));
    }

    @Test
    void sinTokenNoSePuedeListar() throws Exception {
        mockMvc.perform(get("/api/v1/ordenes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenSeListanLasOrdenesPropias() throws Exception {
        mockMvc.perform(get("/api/v1/ordenes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_pedidos.leer"))))
                .andExpect(status().isOk());
    }

    @Test
    void crearUnaOrdenDevuelve201YPublicaElEvento() throws Exception {
        given(catalogo.buscar(eq(1L)))
                .willReturn(new CatalogoClient.ProductoDelCatalogo(1L, "Teclado", 19990));

        mockMvc.perform(post("/api/v1/ordenes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_pedidos.escribir")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"productoId\":1,\"cantidad\":2}]}"))
                .andExpect(status().isCreated())
                // El total lo calcula el servidor con el precio del catalogo:
                // 2 x 19990. Si viniera del cliente, se podria comprar a 1 peso.
                .andExpect(jsonPath("$.total").value(39980));

        verify(publicador).publicarOrdenCreada(any(OrdenCreadaEvento.class));
    }

    @Test
    void unProductoInexistenteDevuelve400YNoPublicaNada() throws Exception {
        given(catalogo.buscar(eq(999L))).willReturn(null);

        mockMvc.perform(post("/api/v1/ordenes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_pedidos.escribir")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"productoId\":999,\"cantidad\":1}]}"))
                .andExpect(status().isBadRequest());

        verify(publicador, never()).publicarOrdenCreada(any());
    }

    @Test
    void unaOrdenSinItemsDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/ordenes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_pedidos.escribir")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest());

        verify(publicador, never()).publicarOrdenCreada(any());
    }

    @Test
    void unaCantidadCeroDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/ordenes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_pedidos.escribir")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"productoId\":1,\"cantidad\":0}]}"))
                .andExpect(status().isBadRequest());

        verify(publicador, never()).publicarOrdenCreada(any());
    }

    @Test
    void verTodasLasOrdenesExigeRolAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/ordenes/todas")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_pedidos.leer"))))
                .andExpect(status().isForbidden());
    }
}
