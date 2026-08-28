package cl.duoc.cloud.auth.model;

import java.util.List;

public record Usuario(
        String username,
        String hashPassword,
        String nombre,
        String email,
        List<String> roles,
        List<String> scopes) {
}
