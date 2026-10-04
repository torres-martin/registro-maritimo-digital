package com.example.registronaves.Model;

public enum Rol {
    ARMADOR("Armador / Naviera", "/mis-solicitudes.html"),
    ASEGURADORA("Aseguradora", "/consulta.html"),
    FUNCIONARIO("Funcionario AMP", "/bandeja.html");

    private final String etiqueta;
    private final String inicio;

    Rol(String etiqueta, String inicio) {
        this.etiqueta = etiqueta;
        this.inicio = inicio;
    }

    public String getEtiqueta() { return etiqueta; }
    public String getInicio() { return inicio; }
}