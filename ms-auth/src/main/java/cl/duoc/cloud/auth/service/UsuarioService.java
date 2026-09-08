package cl.duoc.cloud.auth.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import cl.duoc.cloud.auth.model.Usuario;

/**
 * Almacen de usuarios del Identity Provider propio.
 *
 * El IdP propio se construyo para entender el protocolo y quedo retirado cuando
 * Microsoft Entra ID paso a ser el proveedor de identidad del sistema. Se
 * conserva el codigo, pero sin ningun usuario incorporado: las credenciales de
 * prueba que habia aqui viajaban en el repositorio, y el endpoint de login
 * estaba publicado sin autorizador, de modo que cualquiera podia obtener un
 * token con rol de administrador.
 *
 * Para levantarlo en local hay que declarar los usuarios explicitamente:
 *
 *   IDP_USUARIOS=admin:unaClaveLarga:ROLE_ADMIN,cliente:otraClave:ROLE_USER
 *
 * Sin esa variable no existe ningun usuario y todo intento de login responde 401.
 */
@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final Map<String, Usuario> usuarios = new HashMap<>();
    private final PasswordEncoder codificador;

    public UsuarioService(PasswordEncoder codificador,
            @Value("${idp.usuarios:}") String definicion) {
        this.codificador = codificador;

        for (String entrada : definicion.split(",")) {
            String[] partes = entrada.trim().split(":");
            if (partes.length < 3 || partes[0].isBlank()) {
                continue;
            }
            String nombre = partes[0];
            List<String> roles = List.of(partes).subList(2, partes.length);
            usuarios.put(nombre, new Usuario(
                    nombre,
                    codificador.encode(partes[1]),
                    nombre,
                    nombre + "@local",
                    roles,
                    List.of("productos.leer", "pedidos.leer", "pedidos.escribir")));
        }

        if (usuarios.isEmpty()) {
            log.info("IdP propio sin usuarios configurados: /auth/login rechazara todo intento. "
                    + "El proveedor de identidad del sistema es Microsoft Entra ID.");
        } else {
            log.warn("IdP propio con {} usuario(s) definidos por configuracion. "
                    + "Solo para desarrollo local.", usuarios.size());
        }
    }

    public Optional<Usuario> autenticar(String username, String password) {
        return Optional.ofNullable(usuarios.get(username))
                .filter(u -> codificador.matches(password, u.hashPassword()));
    }
}
