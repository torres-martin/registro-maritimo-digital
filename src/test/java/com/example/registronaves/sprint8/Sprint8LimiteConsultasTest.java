package com.example.registronaves.sprint8;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sprint 8: límite de consultas en la verificación pública (protección contra fuerza bruta).
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.verificacion.max-por-minuto=5")
class Sprint8LimiteConsultasTest {

    @Autowired
    MockMvc mvc;

    private static RequestPostProcessor desdeIp(String ip) {
        return peticion -> {
            peticion.setRemoteAddr(ip);
            return peticion;
        };
    }

    @Test
    @DisplayName("S8-LIM-01: pasado el límite, la verificación responde 429 con Retry-After")
    void pasadoElLimiteResponde429() throws Exception {
        for (int i = 1; i <= 5; i++) {
            mvc.perform(get("/api/publico/verificar/NOEXISTE" + i).with(desdeIp("10.8.0.1")))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/publico/verificar/NOEXISTE6").with(desdeIp("10.8.0.1")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().string(containsString("Demasiadas consultas")));
    }

    @Test
    @DisplayName("S8-LIM-02: el límite es por IP; otra IP sigue pudiendo consultar")
    void otraIpNoSeVeAfectada() throws Exception {
        for (int i = 1; i <= 6; i++) {
            mvc.perform(get("/api/publico/verificar/OTRO" + i).with(desdeIp("10.8.0.2")));
        }
        mvc.perform(get("/api/publico/verificar/OTRO7").with(desdeIp("10.8.0.3")))
                .andExpect(status().isNotFound());
    }
}