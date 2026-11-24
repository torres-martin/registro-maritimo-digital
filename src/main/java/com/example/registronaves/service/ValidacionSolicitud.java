package com.example.registronaves.service;

import com.example.registronaves.exception.DatosInvalidosException;
import com.lowagie.text.pdf.PdfReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

// Sprint 8: validaciones de los datos y del PDF de una solicitud. Se separaron de NaveService
// para que las usen el registro y los borradores.
final class ValidacionSolicitud {

    static final long MAX_PDF_BYTES = 10L * 1024 * 1024;

    private static final Pattern SOLO_DIGITOS = Pattern.compile("^\\d{7}$");

    private ValidacionSolicitud() {
    }

    // Comprueba la cabecera, el cierre y la estructura del PDF antes de guardarlo.
    static void validarPdf(byte[] contenido) {
        String inicio = new String(contenido, 0, Math.min(5, contenido.length), StandardCharsets.US_ASCII);
        int desde = Math.max(0, contenido.length - 1024);
        String finalPdf = new String(contenido, desde, contenido.length - desde, StandardCharsets.US_ASCII);

        if (!inicio.equals("%PDF-") || !finalPdf.stripTrailing().endsWith("%%EOF")) {
            throw new DatosInvalidosException("El archivo adjunto debe ser un PDF válido y completo.");
        }

        PdfReader lector = null;
        try {
            lector = new PdfReader(contenido);
            if (lector.isEncrypted() || lector.isRebuilt() || lector.getNumberOfPages() < 1) {
                throw new DatosInvalidosException("El PDF debe ser válido y no estar protegido con contraseña.");
            }
            for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
                if (lector.getPageN(pagina) == null) {
                    throw new DatosInvalidosException("El archivo adjunto debe ser un PDF válido y completo.");
                }
                lector.getPageContent(pagina);
            }
        } catch (IOException | RuntimeException e) {
            throw new DatosInvalidosException(
                    "El PDF no se pudo leer. Adjunte un PDF válido, completo y sin contraseña.");
        } finally {
            if (lector != null) {
                lector.close();
            }
        }
    }

    static boolean imoValido(String siete) {
        int suma = 0;
        for (int i = 0; i < 6; i++) suma += (siete.charAt(i) - '0') * (7 - i);
        return suma % 10 == siete.charAt(6) - '0';
    }

    static String normalizarImo(String valor) {
        String t = valor.toUpperCase(Locale.ROOT).replace(" ", "");
        return SOLO_DIGITOS.matcher(t).matches() ? "IMO" + t : t;
    }

    static String texto(Map<String, String> c, String clave) {
        String v = c.get(clave);
        return v == null ? "" : v.trim();
    }

    static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    static String requerido(Map<String, String> c, String clave, int max, String etiqueta) {
        String v = texto(c, clave);
        if (v.isEmpty()) throw new DatosInvalidosException(etiqueta + " es obligatorio.");
        if (v.length() > max) throw new DatosInvalidosException(etiqueta + " no puede pasar de " + max + " caracteres.");
        return v;
    }

    static String opcional(Map<String, String> c, String clave, int max, String etiqueta) {
        String v = texto(c, clave);
        if (v.length() > max) throw new DatosInvalidosException(etiqueta + " no puede pasar de " + max + " caracteres.");
        return v.isEmpty() ? null : v;
    }

    static String opcion(Map<String, String> c, String clave, Set<String> validos, String etiqueta) {
        String v = texto(c, clave);
        if (!validos.contains(v)) throw new DatosInvalidosException("Seleccione " + etiqueta + " válido.");
        return v;
    }

    static Double numero(Map<String, String> c, String clave, String etiqueta) {
        String v = texto(c, clave);
        if (v.isEmpty()) return null;
        try {
            double d = Double.parseDouble(v);
            if (d <= 0 || Double.isNaN(d) || Double.isInfinite(d)) throw new NumberFormatException();
            return d;
        } catch (NumberFormatException e) {
            throw new DatosInvalidosException(etiqueta + " debe ser un número mayor que 0.");
        }
    }

    static String fecha(Map<String, String> c, String clave, String etiqueta) {
        String v = texto(c, clave);
        if (v.isEmpty()) throw new DatosInvalidosException(etiqueta + " es obligatoria.");
        try {
            if (LocalDate.parse(v).isAfter(LocalDate.now()))
                throw new DatosInvalidosException(etiqueta + " no puede ser una fecha futura.");
        } catch (DateTimeParseException e) {
            throw new DatosInvalidosException(etiqueta + " no tiene un formato válido.");
        }
        return v;
    }
}
