package com.example.registronaves.service;

import com.example.registronaves.Model.EstadoTramite;
import com.example.registronaves.Model.Nave;
import com.example.registronaves.exception.DatosInvalidosException;
import com.example.registronaves.repository.NaveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Year;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.example.registronaves.service.ValidacionSolicitud.*;

// Sprint 8: registro de una solicitud nueva por el armador (antes en NaveService).
@Service
public class RegistroService {

    static final Path CARPETA_UPLOADS = Paths.get("uploads").toAbsolutePath().normalize();

    private static final String COMPRAVENTA = "Contrato de compraventa";
    private static final Pattern IMO_COMPLETO = Pattern.compile("^IMO\\d{7}$");

    private static final Set<String> TIPOS = Set.of(
            "Portacontenedores", "Granelero", "Tanquero", "Pasajeros", "Carga general");
    private static final Set<String> DOCUMENTOS_BASE = Set.of(
            "Contrato de compraventa", "Certificado de constructor", "Certificado de cancelación de registro",
            "Venta judicial", "Venta extrajudicial", "Contrato de donación");

    private final NaveRepository repo;
    private final AuditoriaService auditoria;

    public RegistroService(NaveRepository repo, AuditoriaService auditoria) {
        this.repo = repo;
        this.auditoria = auditoria;
    }

    @Transactional
    public Nave registrar(Map<String, String> c, MultipartFile documento, String correoArmador) throws IOException {
        Nave nave = new Nave();
        // Documento y partes
        nave.setDocumentoBase(opcion(c, "documentoBase", DOCUMENTOS_BASE, "un documento base"));
        nave.setFechaCelebracion(fecha(c, "fechaCelebracion", "La fecha de celebración del documento"));
        nave.setNombreTransmitente(requerido(c, "nombreTransmitente", 150, "El nombre del transmitente"));
        nave.setPropietario(requerido(c, "propietario", 150, "El nombre del comprador o propietario"));
        if (COMPRAVENTA.equals(nave.getDocumentoBase()))
            nave.setPrecioVenta(numero(c, "precioVenta", "El precio de venta"));
        // Nave
        nave.setNombreNave(requerido(c, "nombreNave", 100, "El nombre de la nave"));
        String imo = normalizarImo(texto(c, "imo"));
        if (!IMO_COMPLETO.matcher(imo).matches())
            throw new DatosInvalidosException("El IMO debe tener 7 dígitos (ejemplo: 1234567 o IMO1234567).");
        if (repo.existeSolicitudActiva(imo))
            throw new DatosInvalidosException("Ya existe una solicitud en revisión o aprobada para el IMO " + imo + ".");

        // DEF-01: el dígito de control del IMO vuelve a validarse.
        if (!imoValido(imo.substring(3)))
            throw new DatosInvalidosException("El número IMO no es válido: el dígito de control no coincide. Revise el certificado de arqueo.");

        nave.setImo(imo);
        nave.setTipoNave(opcion(c, "tipoNave", TIPOS, "un tipo de nave"));
        nave.setDistintivoLlamada(opcional(c, "distintivoLlamada", 20, "El distintivo de llamada"));
        nave.setPatenteNavegacion(opcional(c, "patenteNavegacion", 40, "La patente de navegación"));
        nave.setArqueoBruto(numero(c, "arqueoBruto", "El tonelaje bruto"));
        // Envío
        nave.setSolicitante(requerido(c, "solicitante", 150, "El nombre del solicitante"));
        nave.setAbogado(requerido(c, "abogado", 150, "El abogado a cargo de la inscripción definitiva"));
        nave.setEstado(EstadoTramite.EN_REVISION.getEtiqueta());
        nave.setArmadorCorreo(correoArmador);
        nave.setRutaDocumentoPdf(guardarPdf(documento));

        nave.setTramite("TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        nave = repo.save(nave);
        nave.setTramite(String.format("TR-%d-%04d", Year.now().getValue(), nave.getId()));
        nave = repo.save(nave);
        auditoria.registrar(correoArmador, AuditoriaService.ENVIAR, nave, "IMO " + nave.getImo());
        return nave;
    }

    private String guardarPdf(MultipartFile doc) throws IOException {
        if (doc == null || doc.isEmpty()) {
            throw new DatosInvalidosException("Adjunte el documento en PDF.");
        }
        if (doc.getSize() > MAX_PDF_BYTES) {
            throw new DatosInvalidosException("El PDF no puede pasar de 10 MB.");
        }
        byte[] contenido = doc.getBytes();
        validarPdf(contenido);

        Files.createDirectories(CARPETA_UPLOADS);
        Path destino = CARPETA_UPLOADS.resolve(UUID.randomUUID() + ".pdf");
        Files.write(destino, contenido);
        return destino.toString();
    }
}
