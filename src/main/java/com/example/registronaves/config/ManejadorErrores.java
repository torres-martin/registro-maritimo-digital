package com.example.registronaves.config;

import com.example.registronaves.exception.DatosInvalidosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger LOG =
            LoggerFactory.getLogger(ManejadorErrores.class);

    private static ResponseEntity<Map<String, String>> error(
            HttpStatusCode estado,
            String mensaje
    ) {
        return ResponseEntity.status(estado)
                .body(Map.of("mensaje", mensaje));
    }

    @ExceptionHandler(DatosInvalidosException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(
            DatosInvalidosException e
    ) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> argumentoInvalido(
            IllegalArgumentException e
    ) {
        LOG.warn("Argumento inválido no previsto", e);

        return error(
                HttpStatus.BAD_REQUEST,
                "Los datos enviados no son válidos."
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> cuerpoInvalido(
            HttpMessageNotReadableException e
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "El contenido de la solicitud no tiene un formato válido."
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> accesoDenegado(
            AccessDeniedException e
    ) {
        return error(
                HttpStatus.FORBIDDEN,
                "No tienes permiso para realizar esta acción."
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> sinAutenticacion(
            AuthenticationException e
    ) {
        return error(
                HttpStatus.UNAUTHORIZED,
                "Inicia sesión para continuar."
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> faltaCampo(
            MissingServletRequestParameterException e
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "Falta el campo obligatorio: " + e.getParameterName()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> formatoInvalido(
            MethodArgumentTypeMismatchException e
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "El campo '" + e.getName() + "' tiene un formato inválido."
        );
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(
            NoSuchElementException e
    ) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> archivoGrande(
            MaxUploadSizeExceededException e
    ) {
        return error(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "El PDF no puede pesar más de 10 MB."
        );
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> errorDeArchivo(
            IOException e
    ) {
        LOG.error("Error al acceder al documento", e);

        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo guardar el archivo adjunto."
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> errorInesperado(
            Exception e
    ) {
        // Conserva el estado y las cabeceras HTTP de los errores del cliente.
        if (e instanceof ErrorResponse respuesta
                && respuesta.getStatusCode().is4xxClientError()) {

            String mensaje = switch (respuesta.getStatusCode().value()) {
                case 404 ->
                        "El recurso solicitado no existe.";
                case 405 ->
                        "Esta acción no está disponible para el recurso solicitado.";
                case 415 ->
                        "El formato del contenido enviado no es compatible.";
                default ->
                        "No se pudo procesar la solicitud. Revisa los datos enviados.";
            };

            return ResponseEntity.status(respuesta.getStatusCode())
                    .headers(respuesta.getHeaders())
                    .body(Map.of("mensaje", mensaje));
        }

        // El detalle técnico queda en el registro del servidor.
        LOG.error("Error inesperado al procesar la solicitud", e);

        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Intenta de nuevo más tarde."
        );
    }
}