package cl.duoc.cloud.facturacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ms-facturacion: el microservicio "a eleccion" que pide la pauta.
 *
 * Consume orden.creada y emite la boleta correspondiente (un registro
 * persistido con numero correlativo), igual que lo haria un sistema de
 * facturacion real salvo por la integracion con el SII, fuera de alcance.
 */
@SpringBootApplication
public class FacturacionApplication {

    public static void main(String[] args) {
        SpringApplication.run(FacturacionApplication.class, args);
    }
}
