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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

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
            // DEF-07 (Sprint 7): CSRF activo. El token viaja en la cookie XSRF-TOKEN y el
            // frontend lo devuelve en la cabecera X-XSRF-TOKEN (ver static/csrf.js).
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
            .authorizeHttpRequests(a -> a
                // Públicas
                .requestMatchers("/login.html", "/login.js", "/csrf.js", "/estilos.css", "/img/**", "/verificar.html", "/verificar.js",
        "/favicon.ico", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/publico/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/csrf").permitAll()
                // Pantallas por rol
                .requestMatchers("/nueva-solicitud.html", "/nueva-solicitud.js",
                        "/mis-solicitudes.html", "/mis-solicitudes.js").hasRole("ARMADOR")
                .requestMatchers("/bandeja.html", "/bandeja.js", "/revision.html", "/revision.js",
                        "/bitacora.html", "/bitacora.js").hasRole("FUNCIONARIO")
                .requestMatchers("/consulta.html", "/consulta.js").hasRole("ASEGURADORA")
                // API por rol (las rutas fijas van antes que /api/naves/*)
                .requestMatchers(HttpMethod.POST, "/api/naves").hasRole("ARMADOR")
                .requestMatchers(HttpMethod.GET, "/api/naves/mias").hasRole("ARMADOR")
                .requestMatchers(HttpMethod.GET, "/api/naves/mias/vencimientos").hasRole("ARMADOR")
                .requestMatchers("/api/borradores", "/api/borradores/**").hasRole("ARMADOR")
                .requestMatchers(HttpMethod.GET, "/api/naves/todas").hasRole("FUNCIONARIO")
                .requestMatchers(HttpMethod.GET, "/api/naves/consulta").hasRole("ASEGURADORA")
                .requestMatchers(HttpMethod.PUT, "/api/naves/*/estado").hasRole("FUNCIONARIO")
                .requestMatchers(HttpMethod.PUT, "/api/naves/*/revocar").hasRole("FUNCIONARIO")
                // HU-21: la bitácora solo la consulta el funcionario y no tiene escritura por API
                .requestMatchers(HttpMethod.GET, "/api/auditoria").hasRole("FUNCIONARIO")
                .requestMatchers("/api/auditoria", "/api/auditoria/**").denyAll()
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