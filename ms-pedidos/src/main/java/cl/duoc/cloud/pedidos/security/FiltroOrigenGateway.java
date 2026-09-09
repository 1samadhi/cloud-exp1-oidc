package cl.duoc.cloud.pedidos.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Exige que la peticion venga del API Gateway.
 *
 * El enunciado pide que el API Gateway sea el unico punto de entrada, pero la
 * instancia EC2 tiene IP publica y sus puertos son alcanzables desde Internet:
 * un curl directo se saltaria el gateway y su autorizador. La arquitectura
 * correcta seria una subred privada con VPC Link, que excede el presupuesto
 * del laboratorio.
 *
 * En su lugar, el gateway inyecta una cabecera secreta en cada integracion y
 * este filtro rechaza lo que llegue sin ella. No es aislamiento de red: quien
 * conozca el secreto puede seguir llamando directamente. Lo que consigue es
 * que el gateway sea el unico camino funcional, y que las reglas del borde no
 * se puedan eludir.
 *
 * Se ejecuta antes que la cadena de Spring Security para que el trafico ajeno
 * ni siquiera llegue a la validacion del token.
 */
@Component
@Order(1)
public class FiltroOrigenGateway extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroOrigenGateway.class);
    private static final String CABECERA = "X-Origen-Gateway";

    private final String secreto;

    public FiltroOrigenGateway(@Value("${seguridad.secreto-gateway:}") String secreto) {
        this.secreto = secreto;
    }

    /** Sin secreto configurado el filtro no actua: asi el stack local sigue siendo usable. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        return secreto.isBlank();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
            FilterChain cadena) throws ServletException, IOException {

        if (!secreto.equals(peticion.getHeader(CABECERA))) {
            log.warn("Peticion rechazada por no venir del API Gateway: {} {} desde {}",
                    peticion.getMethod(), peticion.getRequestURI(), peticion.getRemoteAddr());
            respuesta.setStatus(HttpStatus.FORBIDDEN.value());
            respuesta.setContentType("application/json");
            respuesta.getWriter().write(
                    "{\"error\":\"Esta API solo acepta peticiones a traves del API Gateway\"}");
            return;
        }
        cadena.doFilter(peticion, respuesta);
    }
}
