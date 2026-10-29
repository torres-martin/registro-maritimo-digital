package com.example.registronaves.controller;

import com.example.registronaves.service.AuditoriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// HU-21: solo lectura y solo para el funcionario (ver SeguridadConfig).
@RestController
@RequestMapping("/api")
public class AuditoriaController {

    private final AuditoriaService auditoria;

    public AuditoriaController(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @GetMapping("/auditoria")
    public List<Map<String, Object>> listar(@RequestParam(value = "naveId", required = false) Long naveId) {
        return auditoria.listar(naveId);
    }
}
