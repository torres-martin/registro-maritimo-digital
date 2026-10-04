package com.example.registronaves.config;

import com.example.registronaves.Model.Rol;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static String inicioDe(Authentication auth) {
        if (auth != null) {
            for (Rol r : Rol.values()) {
                if (auth.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("ROLE_" + r.name())))
                    return r.getInicio();
            }
        }
        return "/login.html";
    }

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
            // CSRF desactivado a propósito por ahora (se documenta como pendiente)
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(a -> a
                // Públicas
                .requestMatchers("/login.html", "/login.js", "/estilos.css", "/verificar.html", "/verificar.js",
                        "/favicon.ico", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/publico/**").permitAll()
                // Pantallas por rol
                .requestMatchers("/nueva-solicitud.html", "/nueva-solicitud.js",
                        "/mis-solicitudes.html", "/mis-solicitudes.js").hasRole("ARMADOR")
                .requestMatchers("/bandeja.html", "/bandeja.js", "/revision.html", "/revision.js").hasRole("FUNCIONARIO")
                .requestMatchers("/consulta.html", "/consulta.js").hasRole("ASEGURADORA")
                // API por rol (las rutas fijas van antes que /api/naves/*)
                .requestMatchers(HttpMethod.POST, "/api/naves").hasRole("ARMADOR")
                .requestMatchers(HttpMethod.GET, "/api/naves/mias").hasRole("ARMADOR")
                .requestMatchers(HttpMethod.GET, "/api/naves/todas").hasRole("FUNCIONARIO")
                .requestMatchers(HttpMethod.GET, "/api/naves/consulta").hasRole("ASEGURADORA")
                .requestMatchers(HttpMethod.PUT, "/api/naves/*/estado").hasRole("FUNCIONARIO")
                .requestMatchers(HttpMethod.GET, "/api/naves/*", "/api/naves/*/documento", "/api/naves/*/certificado").hasAnyRole("ARMADOR", "FUNCIONARIO")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> {
                    if (req.getRequestURI().startsWith("/api/")) {
                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    } else {
                        res.sendRedirect("/login.html");
                    }
                })
                .accessDeniedHandler((req, res, ex) -> {
                    if (req.getRequestURI().startsWith("/api/")) {
                        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    } else {
                        res.sendRedirect(inicioDe(SecurityContextHolder.getContext().getAuthentication()));
                    }
                }));
        return http.build();
    }
}