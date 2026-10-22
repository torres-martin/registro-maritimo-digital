package com.example.registronaves.exception;

// Representa una validación de negocio cuyo mensaje puede mostrarse al usuario. //

public class DatosInvalidosException extends IllegalArgumentException {

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}