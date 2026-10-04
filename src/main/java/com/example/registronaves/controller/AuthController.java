package com.example.registronaves.controller;

import com.example.registronaves.Model.Usuario;
import com.example.registronaves.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final int MAX_INTENTOS = 3;
    private static final int MINUTOS_BLOQUEO = 15;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final SecurityContextRepository contextos = new HttpSessionSecurityContextRepository();

    public AuthController(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    public record LoginRequest(String correo, String password) {}

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest req,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        String correo = req.correo() == null ? "" : req.correo().trim().toLowerCase();
        String clave = req.password() == null ? "" : req.password();

        Usuario u = usuarios.findByCorreo(correo).orElse(null);

        if (u != null && u.getBloqueadoHasta() != null && u.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
            long min = Duration.between(LocalDateTime.now(), u.getBloqueadoHasta()).toMinutes() + 1;
            return error(HttpStatus.LOCKED, "Cuenta bloqueada temporalmente. Intenta de nuevo en " + min + " minuto(s).");
        }

        boolean correcto = u != null && encoder.matches(clave, u.getPasswordHash());

        if (!correcto) {
            if (u == null) {
                return error(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
            }
            u.setIntentosFallidos(u.getIntentosFallidos() + 1);
            if (u.getIntentosFallidos() >= MAX_INTENTOS) {
                u.setBloqueadoHasta(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
                u.setIntentosFallidos(0);
                usuarios.save(u);
                return error(HttpStatus.LOCKED, "Demasiados intentos. La cuenta se bloqueó por " + MINUTOS_BLOQUEO + " minutos.");
            }
            usuarios.save(u);
            int quedan = MAX_INTENTOS - u.getIntentosFallidos();
            return error(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos. Te quedan " + quedan
                    + " intento(s) antes de que la cuenta se bloquee por " + MINUTOS_BLOQUEO + " minutos.");
        }

        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        usuarios.save(u);

        // Evita fijación de sesión: si ya había sesión, se cambia su identificador
        if (request.getSession(false) != null) request.changeSessionId();

        var autoridad = new SimpleGrantedAuthority("ROLE_" + u.getRol().name());
        var auth = UsernamePasswordAuthenticationToken.authenticated(u.getCorreo(), null, List.of(autoridad));
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(auth);
        SecurityContextHolder.setContext(contexto);
        contextos.saveContext(contexto, request, response);

        return ResponseEntity.ok(Map.<String, Object>of(
                "nombre", u.getNombre(),
                "rol", u.getRol().name(),
                "destino", u.getRol().getInicio()));
    }

    @GetMapping("/yo")
    public Map<String, String> yo(Authentication auth) {
        Usuario u = usuarios.findByCorreo(auth.getName()).orElseThrow();
        return Map.of("nombre", u.getNombre(),
                      "rol", u.getRol().name(),
                      "rolEtiqueta", u.getRol().getEtiqueta());
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) sesion.invalidate();
        SecurityContextHolder.clearContext();
        return Map.of("mensaje", "Sesión cerrada.");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.<String, Object>of("mensaje", mensaje));
    }
}