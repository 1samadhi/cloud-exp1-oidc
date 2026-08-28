package cl.duoc.cloud.auth.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class EstadoController {

    @GetMapping("/estado")
    public Map<String, String> estado() {
        return Map.of(
                "servicio", "ms-auth",
                "version", "1.0.0",
                "estado", "operativo");
    }
}
