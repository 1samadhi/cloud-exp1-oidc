package cl.duoc.cloud.pedidos.controller;

import static org.mockito.BDDMockito.given;
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

import cl.duoc.cloud.pedidos.service.CatalogoClient;

@SpringBootTest
class PedidoV1ControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    @MockitoBean
    private CatalogoClient catalogo;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    void elEndpointPublicoNoExigeToken() throws Exception {
        mockMvc.perform(get("/api/v1/public"))
                .andExpect(status().isOk());
    }

    @Test
    void crearSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productoId\":1,\"cantidad\":2}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearSinElScopeNecesarioDevuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/pedidos")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_pedidos.leer")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productoId\":1,\"cantidad\":2}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearConElScopeCorrectoDevuelve201() throws Exception {
        given(catalogo.existeProducto(1L)).willReturn(true);
        mockMvc.perform(post("/api/v1/pedidos")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_pedidos.escribir")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productoId\":1,\"cantidad\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidad").value(2));
    }

    @Test
    void crearConProductoInexistenteDevuelve400() throws Exception {
        given(catalogo.existeProducto(999L)).willReturn(false);
        mockMvc.perform(post("/api/v1/pedidos")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_pedidos.escribir")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productoId\":999,\"cantidad\":1}"))
                .andExpect(status().isBadRequest());
    }
}
