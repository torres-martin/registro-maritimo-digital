package com.example.registronaves.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Sprint 8: limita las consultas a la verificación pública para evitar
// que alguien pruebe códigos de certificado por fuerza bruta.
@Component
public class LimiteConsultasFilter extends OncePerRequestFilter {

    private static final long VENTANA_MS = 60_000;
    private final int maximo;
    private final Map<String, Ventana> porIp = new ConcurrentHashMap<>();

    public LimiteConsultasFilter(@Value("${app.verificacion.max-por-minuto:10}") int maximo) {
        this.maximo = maximo;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !req.getRequestURI().startsWith("/api/publico/verificar/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long ahora = System.currentTimeMillis();
        if (porIp.size() > 10_000) {
            porIp.values().removeIf(v -> ahora - v.inicio >= VENTANA_MS);
        }
        Ventana v = porIp.compute(req.getRemoteAddr(), (ip, actual) ->
                actual == null || ahora - actual.inicio >= VENTANA_MS ? new Ventana(ahora) : actual);

        if (v.contador.incrementAndGet() > maximo) {
            long espera = Math.max(1, (v.inicio + VENTANA_MS - ahora) / 1000);
            res.setStatus(429);
            res.setHeader("Retry-After", String.valueOf(espera));
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"mensaje\":\"Demasiadas consultas. Intenta de nuevo en "
                    + espera + " segundos.\"}");
            return;
        }
        chain.doFilter(req, res);
    }

    private static final class Ventana {
        final long inicio;
        final AtomicInteger contador = new AtomicInteger();
        Ventana(long inicio) { this.inicio = inicio; }
    }
}