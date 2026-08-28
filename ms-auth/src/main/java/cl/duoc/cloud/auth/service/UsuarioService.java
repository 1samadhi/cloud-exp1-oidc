package cl.duoc.cloud.auth.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import cl.duoc.cloud.auth.model.Usuario;

/**
 * Almacen de usuarios simulado, como permite la actividad. Las contrasenias no
 * se guardan en claro: se codifican con BCrypt al arrancar.
 */
@Service
public class UsuarioService {

    private final Map<String, Usuario> usuarios;
    private final PasswordEncoder codificador;

    public UsuarioService(PasswordEncoder codificador) {
        this.codificador = codificador;
        this.usuarios = Map.of(
                "admin", new Usuario(
                        "admin", codificador.encode("admin123"),
                        "Administrador EXP1", "admin@duocuc.cl",
                        List.of("ROLE_ADMIN", "ROLE_USER"),
                        List.of("productos.leer", "productos.escribir", "pedidos.leer", "pedidos.escribir")),
                "cliente", new Usuario(
                        "cliente", codificador.encode("cliente123"),
                        "Cliente de prueba", "cliente@duocuc.cl",
                        List.of("ROLE_USER"),
                        List.of("productos.leer", "pedidos.leer", "pedidos.escribir")));
    }

    public Optional<Usuario> autenticar(String username, String password) {
        return Optional.ofNullable(usuarios.get(username))
                .filter(u -> codificador.matches(password, u.hashPassword()));
    }
}
