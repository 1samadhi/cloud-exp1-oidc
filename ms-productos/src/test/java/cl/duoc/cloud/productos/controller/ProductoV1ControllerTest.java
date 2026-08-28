package cl.duoc.cloud.productos.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class ProductoV1ControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void listarDevuelveElCatalogo() throws Exception {
        mockMvc().perform(get("/api/v1/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Teclado mecanico"));
    }

    @Test
    void obtenerProductoExistenteDevuelve200() throws Exception {
        mockMvc().perform(get("/api/v1/productos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void obtenerProductoInexistenteDevuelve404() throws Exception {
        mockMvc().perform(get("/api/v1/productos/999"))
                .andExpect(status().isNotFound());
    }
}
