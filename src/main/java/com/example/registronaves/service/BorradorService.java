package com.example.registronaves.service;

import com.example.registronaves.Model.BorradorSolicitud;
import com.example.registronaves.Model.EstadoTramite;
import com.example.registronaves.Model.Nave;
import com.example.registronaves.exception.DatosInvalidosException;
import com.example.registronaves.repository.BorradorRepository;
import com.example.registronaves.repository.NaveRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.regex.Pattern;

// Sprint 8: borradores del armador y su envío como solicitud (antes en NaveService).
@Service
public class BorradorService {

    private static final int MAX_DATOS = 12000;
    private static final Pattern UUID_CANONICO = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Map<String, Integer> CAMPOS = Map.ofEntries(
            Map.entry("documentoBase", 150), Map.entry("fechaCelebracion", 40),
            Map.entry("precioVenta", 40), Map.entry("nombreTransmitente", 150),
            Map.entry("propietario", 150), Map.entry("nombreNave", 100),
            Map.entry("imo", 30), Map.entry("tipoNave", 60),
            Map.entry("distintivoLlamada", 20), Map.entry("patenteNavegacion", 40),
            Map.entry("arqueoBruto", 40), Map.entry("solicitante", 150), Map.entry("abogado", 150));
    private static final String CONFLICTO = "El borrador cambió en otra sesión. Recárgalo antes de continuar.";

    private final BorradorRepository borradores;
    private final NaveRepository repo;
    private final RegistroService registro;
    private final ObjectMapper json;
    private final AuditoriaService auditoria;

    public BorradorService(BorradorRepository borradores, NaveRepository repo, RegistroService registro,
                           ObjectMapper json, AuditoriaService auditoria) {
        this.borradores = borradores;
        this.repo = repo;
        this.registro = registro;
        this.json = json;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarBorradores(String correo) {
        return borradores.listarResumen(correo).stream().map(b -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", b.getId());
            r.put("version", b.getVersion());
            r.put("nombreNave", b.getNombreNave());
            r.put("paso", b.getPaso());
            r.put("actualizado", b.getActualizado());
            r.put("tieneDocumento", b.getTieneDocumento());
            return r;
        }).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detalleBorrador(String id, String correo) {
        return detalleBorrador(buscarBorrador(id, correo));
    }

    @Transactional(rollbackFor = IOException.class)
    public Map<String, Object> guardarBorrador(String id, String datos, int paso, long version,
                                               Long origenSolicitudId, boolean quitarDocumento,
                                               MultipartFile documento, String correo) throws IOException {
        String uuid = uuidBorrador(id);
        validarVersionBorrador(version);
        if (paso < 1 || paso > 3)
            throw new DatosInvalidosException("El paso debe estar entre 1 y 3.");
        Map<String, String> campos = leerDatosBorrador(datos);
        validarOrigenBorrador(origenSolicitudId, correo);
        if (quitarDocumento && documento != null && !documento.isEmpty())
            throw new DatosInvalidosException("Elige entre adjuntar o quitar el documento.");

        BorradorSolicitud borrador = borradores.buscarParaActualizar(uuid, correo).orElse(null);
        if (borrador == null) {
            // No permite que otro armador reutilice un identificador existente.
            if (borradores.existsById(uuid)) throw borradorNoEncontrado();
            if (version != 0) throw new ResponseStatusException(HttpStatus.CONFLICT, CONFLICTO);
            borrador = new BorradorSolicitud();
            borrador.setId(uuid);
            borrador.setArmadorCorreo(correo);
            // La versión se deja null: JPA identifica así el UUID asignado como nuevo.
        } else {
            comprobarVersionBorrador(borrador, version);
        }

        if (quitarDocumento) {
            borrador.setPdf(null);
            borrador.setNombreDocumento(null);
            borrador.setTamanoDocumento(null);
        } else if (documento != null && !documento.isEmpty()) {
            byte[] contenido = leerPdfBorrador(documento);
            borrador.setPdf(contenido);
            borrador.setNombreDocumento(nombreDocumentoBorrador(documento.getOriginalFilename()));
            borrador.setTamanoDocumento((long) contenido.length);
        }
        borrador.setDatosJson(json.writeValueAsString(campos));
        borrador.setNombreNave(campos.getOrDefault("nombreNave", "").trim());
        borrador.setPaso(paso);
        borrador.setOrigenSolicitudId(origenSolicitudId);
        borrador.setActualizado(Instant.now());
        try {
            return detalleBorrador(borradores.saveAndFlush(borrador));
        } catch (DataIntegrityViolationException e) {
            // Una creación simultánea con el mismo UUID debe dar conflicto, nunca duplicarse.
            throw new ResponseStatusException(HttpStatus.CONFLICT, CONFLICTO, e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] documentoBorrador(String id, String correo) {
        BorradorSolicitud borrador = buscarBorrador(id, correo);
        if (borrador.getPdf() == null) throw new NoSuchElementException("El borrador no tiene documento adjunto.");
        return borrador.getPdf();
    }

    @Transactional
    public void eliminarBorrador(String id, long version, String correo) {
        validarVersionBorrador(version);
        BorradorSolicitud borrador = bloquearBorrador(id, correo);
        comprobarVersionBorrador(borrador, version);
        borradores.delete(borrador);
        borradores.flush();
    }

    @Transactional(rollbackFor = IOException.class)
    public Map<String, Object> enviarBorrador(String id, Map<String, String> campos, long version,
                                              MultipartFile documento, String correo) throws IOException {
        validarVersionBorrador(version);
        BorradorSolicitud borrador = bloquearBorrador(id, correo);
        comprobarVersionBorrador(borrador, version);
        validarOrigenBorrador(borrador.getOrigenSolicitudId(), correo);
        MultipartFile adjunto = documento;
        if ((adjunto == null || adjunto.isEmpty()) && borrador.getPdf() != null)
            adjunto = new DocumentoBorrador(borrador.getNombreDocumento(), borrador.getPdf());

        Nave nave = registro.registrar(campos, adjunto, correo);
        Long origenId = borrador.getOrigenSolicitudId();
        if (origenId != null) {
            // DEF-03: la corrección queda enlazada a la solicitud rechazada.
            Nave origen = repo.findById(origenId).orElseThrow(() -> new NoSuchElementException("Solicitud de origen no encontrada."));
            nave.setSolicitudOrigenId(origen.getId());
            nave.setTramiteOrigen(origen.getTramite());
            nave = repo.save(nave);
            auditoria.registrar(correo, AuditoriaService.CORREGIR, nave, "Corrige " + origen.getTramite());
        }
        borradores.delete(borrador);
        borradores.flush();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("tramite", nave.getTramite());
        r.put("estado", nave.getEstado());
        r.put("nombreNave", nave.getNombreNave());
        r.put("tramiteOrigen", nave.getTramiteOrigen());
        return r;
    }

    private BorradorSolicitud buscarBorrador(String id, String correo) {
        return borradores.findByIdAndArmadorCorreo(uuidBorrador(id), correo).orElseThrow(BorradorService::borradorNoEncontrado);
    }

    private BorradorSolicitud bloquearBorrador(String id, String correo) {
        return borradores.buscarParaActualizar(uuidBorrador(id), correo).orElseThrow(BorradorService::borradorNoEncontrado);
    }

    private Map<String, Object> detalleBorrador(BorradorSolicitud b) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", b.getId());
        r.put("version", b.getVersion());
        r.put("paso", b.getPaso());
        r.put("datos", leerDatosBorrador(b.getDatosJson()));
        r.put("origenSolicitudId", b.getOrigenSolicitudId());
        r.put("tieneDocumento", b.getTamanoDocumento() != null);
        r.put("nombreDocumento", b.getNombreDocumento());
        r.put("tamanoDocumento", b.getTamanoDocumento());
        r.put("actualizado", b.getActualizado());
        return r;
    }

    private Map<String, String> leerDatosBorrador(String datos) {
        if (datos == null || datos.length() > MAX_DATOS)
            throw new DatosInvalidosException("Los datos del borrador no pueden pasar de 12000 caracteres.");
        JsonNode objeto;
        try {
            objeto = json.readTree(datos);
        } catch (RuntimeException e) {
            throw new DatosInvalidosException("Los datos del borrador deben ser un objeto JSON válido.");
        }
        if (objeto == null || !objeto.isObject())
            throw new DatosInvalidosException("Los datos del borrador deben ser un objeto JSON válido.");
        Map<String, String> campos = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> campo : objeto.properties()) {
            Integer maximo = CAMPOS.get(campo.getKey());
            if (maximo == null || !campo.getValue().isString())
                throw new DatosInvalidosException("El borrador contiene un campo o valor no permitido.");
            String valor = campo.getValue().asString();
            if (valor.length() > maximo)
                throw new DatosInvalidosException("El campo '" + campo.getKey() + "' no puede pasar de " + maximo + " caracteres.");
            campos.put(campo.getKey(), valor);
        }
        return campos;
    }

    private void validarOrigenBorrador(Long id, String correo) {
        if (id == null) return;
        Nave origen = repo.findById(id).orElseThrow(() -> new NoSuchElementException("Solicitud de origen no encontrada."));
        if (!correo.equals(origen.getArmadorCorreo()))
            throw new NoSuchElementException("Solicitud de origen no encontrada.");
        if (!EstadoTramite.RECHAZADO.getEtiqueta().equals(origen.getEstado()))
            throw new DatosInvalidosException("Solo se puede corregir una solicitud rechazada.");
    }

    private static byte[] leerPdfBorrador(MultipartFile documento) throws IOException {
        if (documento.getSize() > ValidacionSolicitud.MAX_PDF_BYTES)
            throw new DatosInvalidosException("El PDF no puede pasar de 10 MB.");
        byte[] contenido = documento.getBytes();
        if (contenido.length > ValidacionSolicitud.MAX_PDF_BYTES)
            throw new DatosInvalidosException("El PDF no puede pasar de 10 MB.");
        ValidacionSolicitud.validarPdf(contenido);
        return contenido;
    }

    private static String nombreDocumentoBorrador(String nombre) {
        if (nombre == null || nombre.isBlank()) return "documento.pdf";
        String seguro = nombre.replace('\\', '/');
        seguro = seguro.substring(seguro.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (seguro.isBlank()) return "documento.pdf";
        return seguro.length() > 255 ? seguro.substring(0, 255) : seguro;
    }

    private static String uuidBorrador(String id) {
        if (id == null || !UUID_CANONICO.matcher(id).matches())
            throw new DatosInvalidosException("El identificador del borrador no es válido.");
        return UUID.fromString(id).toString();
    }

    private static void validarVersionBorrador(long version) {
        if (version < 0) throw new DatosInvalidosException("La versión del borrador no es válida.");
    }

    private static void comprobarVersionBorrador(BorradorSolicitud borrador, long version) {
        if (borrador.getVersion() == null || borrador.getVersion() != version)
            throw new ResponseStatusException(HttpStatus.CONFLICT, CONFLICTO);
    }

    private static NoSuchElementException borradorNoEncontrado() {
        return new NoSuchElementException("Borrador no encontrado.");
    }

    private static final class DocumentoBorrador implements MultipartFile {
        private final String nombre;
        private final byte[] contenido;

        private DocumentoBorrador(String nombre, byte[] contenido) {
            this.nombre = nombre;
            this.contenido = contenido;
        }

        @Override public String getName() { return "documento"; }
        @Override public String getOriginalFilename() { return nombre; }
        @Override public String getContentType() { return "application/pdf"; }
        @Override public boolean isEmpty() { return contenido.length == 0; }
        @Override public long getSize() { return contenido.length; }
        @Override public byte[] getBytes() { return contenido; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(contenido); }
        @Override public void transferTo(File destino) throws IOException { Files.write(destino.toPath(), contenido); }
    }
}
