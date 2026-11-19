package com.example.registronaves.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// DEF-07: renueva la cookie XSRF-TOKEN cuando el navegador no la tiene (por ejemplo,
// después de borrar las cookies). La cookie la escribe SpaCsrfTokenRequestHandler.
@RestController
public class CsrfController {

    @GetMapping("/api/csrf")
    public ResponseEntity<Void> renovar() {
        return ResponseEntity.noContent().header("Cache-Control", "no-store").build();
    }
}
