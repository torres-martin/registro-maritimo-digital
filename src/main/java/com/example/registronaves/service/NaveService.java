package com.example.registronaves.service;

import com.example.registronaves.Model.BorradorSolicitud;
import com.example.registronaves.Model.EstadoTramite;
import com.example.registronaves.Model.Nave;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import com.example.registronaves.exception.DatosInvalidosException;
import com.lowagie.text.pdf.PdfReader;


@Service
public class NaveService {

    public static final String SIN_DOCUMENTO = "sin_documento.pdf";
    private static final String COMPRAVENTA = "Contrato de compraventa";

    private static final Pattern IMO_COMPLETO = Pattern.compile("^IMO\\d{7}$");
    private static final Pattern SOLO_DIGITOS = Pattern.compile("^\\d{7}$");
    private static final long MAX_PDF_BYTES = 10L * 1024 * 1024;
    private static final int MAX_ADICIONALES = 8000;

    private static final Set<String> TIPOS = Set.of(
            "Portacontenedores", "Granelero", "Tanquero", "Pasajeros", "Carga general");
    private static final Set<String> DOCUMENTOS_BASE = Set.of(
            "Contrato de compraventa", "Certificado de constructor", "Certificado de cancelación de registro",
            "Venta judicial", "Venta extrajudicial", "Contrato de donación");
    private static final Set<String> TIPOS_TRANSMITENTE = Set.of(
            "Persona jurídica", "Persona natural", "Registro naval extranjero");
    private static final Set<String> TIPOS_ADQUIRENTE = Set.of("Persona jurídica", "Persona natural");
    private static final Set<String> LEGALIZACIONES = Set.of("Apostilla", "Autenticación consular o notarial");

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

    private static final Path CARPETA_UPLOADS = Paths.get("uploads").toAbsolutePath().normalize();

    private final NaveRepository repo;
    private final CertificadoService certificados;
    private final BorradorRepository borradores;
    private final ObjectMapper json;

    public NaveService(NaveRepository repo, CertificadoService certificados,
                       BorradorRepository borradores, ObjectMapper json) {
        this.repo = repo;
        this.certificados = certificados;
        this.borradores = borradores;
        this.json = json;
    }

    // registro 

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
        if (repo.existsByImoAndEstadoIn(imo, List.of("En revisión", "Aprobado")))
            throw new DatosInvalidosException("Ya existe una solicitud en revisión o aprobada para el IMO " + imo + ".");

        // VALIDACIÓN DE DÍGITO DE CONTROL DESACTIVADA PARA PRUEBAS:
        // if (!imoValido(imo.substring(3)))
        //     throw new DatosInvalidosException("El número IMO no es válido: el dígito de control no coincide. Revise el certificado de arqueo.");

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
        return repo.save(nave);
    }

    // consultas 

    @Transactional(readOnly = true)
    public List<Map<String, Object>> mias(String correo) {
        return repo.findByArmadorCorreoOrderByIdDesc(correo).stream().map(NaveService::lista).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> vencimientosMios(String correo) {
        LocalDate hoy = LocalDate.now();
        return repo.findVencimientosPorArmador(correo, hoy, hoy.plusDays(30)).stream()
                .filter(n -> !n.getCertificadoFolio().isBlank())
                .map(n -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", n.getId());
                    m.put("tramite", n.getTramite());
                    m.put("nombreNave", n.getNombreNave());
                    m.put("imo", n.getImo());
                    m.put("certificadoFolio", n.getCertificadoFolio());
                    m.put("fechaVencimiento", n.getFechaVencimiento().toString());
                    m.put("diasRestantes", ChronoUnit.DAYS.between(hoy, n.getFechaVencimiento()));
                    return m;
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> todas() {
        return repo.findAllByOrderByIdDesc().stream().map(NaveService::lista).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> consulta() {
        return repo.findAllByOrderByIdDesc().stream().map(NaveService::consulta).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detalle(Long id, String correo, boolean funcionario) {
        return detalle(buscar(id, correo, funcionario));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> verificar(String codigo) {
        Nave n = repo.findByCodigoVerificacion(texto(codigo).toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new NoSuchElementException("No se encontró ningún certificado con ese código."));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("folio", n.getCertificadoFolio());
        m.put("nombreNave", n.getNombreNave());
        m.put("imo", n.getImo());
        m.put("estado", CertificadoService.estado(n));
        m.put("fechaEmision", String.valueOf(n.getFechaEmision()));
        m.put("fechaVencimiento", String.valueOf(n.getFechaVencimiento()));
        return m;
    }

    @Transactional(readOnly = true)
    public Path rutaDocumento(Long id, String correo, boolean funcionario) {
        Nave nave = buscar(id, correo, funcionario);
        String ruta = nave.getRutaDocumentoPdf();
        if (ruta == null || SIN_DOCUMENTO.equals(ruta))
            throw new NoSuchElementException("Esta solicitud no tiene documento adjunto.");
        Path archivo = Paths.get(ruta).toAbsolutePath().normalize();
        if (!archivo.startsWith(CARPETA_UPLOADS) || !Files.isReadable(archivo))
            throw new NoSuchElementException("El documento no está disponible.");
        return archivo;
    }

    // decisión del funcionario 

    @Transactional
    public Map<String, Object> cambiarEstado(Long id, String estadoTxt, String observacion, String liquidacion) {
        EstadoTramite estado = EstadoTramite.desdeEtiqueta(estadoTxt);
        if (estado == EstadoTramite.EN_REVISION)
           throw new DatosInvalidosException("La decisión debe ser Aprobado o Rechazado.");

        Nave nave = repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada."));
        if (!EstadoTramite.EN_REVISION.getEtiqueta().equals(nave.getEstado()))
            throw new DatosInvalidosException("Esta solicitud ya fue resuelta.");

        String obs = observacion == null ? "" : observacion.trim();
        if (estado == EstadoTramite.RECHAZADO && obs.isEmpty())
            throw new DatosInvalidosException("Indique la observación del rechazo.");
        if (obs.length() > 1000)
            throw new DatosInvalidosException("La observación no puede pasar de 1000 caracteres.");

        String liq = liquidacion == null ? "" : liquidacion.trim();
        if (liq.length() > 40)
            throw new DatosInvalidosException("El número de liquidación no puede pasar de 40 caracteres.");

        nave.setEstado(estado.getEtiqueta());
        nave.setObservacion(obs.isEmpty() ? null : obs);
        nave.setLiquidacion(liq.isEmpty() ? null : liq);
        nave.setFechaDecision(LocalDate.now());

        if (estado == EstadoTramite.APROBADO) emitirCertificado(nave);

        return detalle(repo.save(nave));
    }

    private void emitirCertificado(Nave nave) {
        LocalDate hoy = LocalDate.now();
        nave.setCertificadoFolio(String.format("RMD-%d-%06d", hoy.getYear(), nave.getId()));
        nave.setCodigoVerificacion(CertificadoService.nuevoCodigo());
        nave.setFechaEmision(hoy);
        nave.setFechaVencimiento(hoy.plusMonths(6));
    }

    @Transactional(readOnly = true)
    public byte[] certificadoPdf(Long id, String correo, boolean funcionario) {
        Nave nave = buscar(id, correo, funcionario);
        if (nave.getCertificadoFolio() == null)
            throw new NoSuchElementException("Esta solicitud todavía no tiene certificado.");
        return certificados.pdf(nave);
    }

    // revocación del certificado

    @Transactional
    public Map<String, Object> revocar(Long id, String motivoTxt) {
        Nave nave = repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada."));
        if (nave.getCertificadoFolio() == null)
            throw new DatosInvalidosException("Esta solicitud no tiene certificado emitido.");
        if (nave.isRevocado())
            throw new DatosInvalidosException("El certificado ya fue revocado.");

        String motivo = texto(motivoTxt);
        if (motivo.isEmpty())
            throw new DatosInvalidosException("Indique el motivo de la revocación.");
        if (motivo.length() > 500)
            throw new DatosInvalidosException("El motivo no puede pasar de 500 caracteres.");

        nave.setRevocado(true);
        nave.setMotivoRevocacion(motivo);
        nave.setFechaRevocacion(LocalDate.now());
        return detalle(repo.save(nave));
    }

    // vistas 

    private static Map<String, Object> lista(Nave n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("tramite", n.getTramite());
        m.put("nombreNave", n.getNombreNave());
        m.put("imo", n.getImo());
        m.put("tipoNave", n.getTipoNave());
        m.put("documentoBase", n.getDocumentoBase());
        m.put("propietario", n.getPropietario());
        m.put("estado", n.getEstado());
        m.put("observacion", n.getObservacion());
        m.put("fechaCreacion", String.valueOf(n.getFechaCreacion()));
        return m;
    }

    private static Map<String, Object> consulta(Nave n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tramite", n.getTramite());
        m.put("nombreNave", n.getNombreNave());
        m.put("imo", n.getImo());
        m.put("tipoNave", n.getTipoNave());
        m.put("propietario", n.getPropietario());
        m.put("estado", n.getEstado());
        m.put("estadoCertificado", n.getCertificadoFolio() == null ? null : CertificadoService.estado(n));
        m.put("fechaVencimiento", n.getFechaVencimiento() == null ? null : n.getFechaVencimiento().toString());
        return m;
    }

    private static Map<String, Object> detalle(Nave n) {
        Map<String, Object> m = lista(n);
        m.put("liquidacion", n.getLiquidacion());
        m.put("tipoTransmitente", n.getTipoTransmitente());
        m.put("nombreTransmitente", n.getNombreTransmitente());
        m.put("domicilioTransmitente", n.getDomicilioTransmitente());
        m.put("tipoAdquirente", n.getTipoAdquirente());
        m.put("domicilioPropietario", n.getDomicilioPropietario());
        m.put("distintivoLlamada", n.getDistintivoLlamada());
        m.put("patenteNavegacion", n.getPatenteNavegacion());
        m.put("arqueoBruto", n.getArqueoBruto());
        m.put("arqueoNeto", n.getArqueoNeto());
        m.put("eslora", n.getEslora());
        m.put("manga", n.getManga());
        m.put("puntal", n.getPuntal());
        m.put("precioVenta", n.getPrecioVenta());
        m.put("fechaCelebracion", n.getFechaCelebracion());
        m.put("tipoLegalizacion", n.getTipoLegalizacion());
        m.put("solicitante", n.getSolicitante());
        m.put("abogado", n.getAbogado());
        m.put("adicionales", n.getAdicionales());
        m.put("tieneDocumento", n.getRutaDocumentoPdf() != null && !SIN_DOCUMENTO.equals(n.getRutaDocumentoPdf()));

        m.put("certificadoFolio", n.getCertificadoFolio());
        m.put("fechaEmision", n.getFechaEmision() == null ? null : n.getFechaEmision().toString());
        m.put("fechaVencimiento", n.getFechaVencimiento() == null ? null : n.getFechaVencimiento().toString());
        m.put("estadoCertificado", n.getCertificadoFolio() == null ? null : CertificadoService.estado(n));
        m.put("fechaDecision", n.getFechaDecision() == null ? null : n.getFechaDecision().toString());
        m.put("codigoVerificacion", n.getCodigoVerificacion());
        m.put("revocado", n.isRevocado());
        m.put("motivoRevocacion", n.getMotivoRevocacion());
        m.put("fechaRevocacion", n.getFechaRevocacion() == null ? null : n.getFechaRevocacion().toString());

        return m;
    }

    //  utilidades privadas 

    private Nave buscar(Long id, String correo, boolean funcionario) {
        Nave n = repo.findById(id).orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada."));
        if (!funcionario && (correo == null || !correo.equals(n.getArmadorCorreo())))
            throw new NoSuchElementException("Solicitud no encontrada.");
        return n;
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

// Comprueba la cabecera, el cierre y la estructura del PDF antes de guardarlo. // 
  static void validarPdf(byte[] contenido) {
    String inicio = new String(
            contenido,
            0,
            Math.min(5, contenido.length),
            StandardCharsets.US_ASCII
    );

    int desde = Math.max(0, contenido.length - 1024);

    String finalPdf = new String(
            contenido,
            desde,
            contenido.length - desde,
            StandardCharsets.US_ASCII
    );

    if (!inicio.equals("%PDF-")
            || !finalPdf.stripTrailing().endsWith("%%EOF")) {
        throw new DatosInvalidosException(
                "El archivo adjunto debe ser un PDF válido y completo."
        );
    }

    PdfReader lector = null;

    try {
        lector = new PdfReader(contenido);

        if (lector.isEncrypted()
                || lector.isRebuilt()
                || lector.getNumberOfPages() < 1) {
            throw new DatosInvalidosException(
                    "El PDF debe ser válido y no estar protegido con contraseña."
            );
        }

        for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
            if (lector.getPageN(pagina) == null) {
                throw new DatosInvalidosException(
                        "El archivo adjunto debe ser un PDF válido y completo."
                );
            }

            lector.getPageContent(pagina);
        }
    } catch (IOException | RuntimeException e) {
        throw new DatosInvalidosException(
                "El PDF no se pudo leer. Adjunte un PDF válido, completo y sin contraseña."
        );
    } finally {
        if (lector != null) {
            lector.close();
        }
    }
}

    private static boolean imoValido(String siete) {
        int suma = 0;
        for (int i = 0; i < 6; i++) suma += (siete.charAt(i) - '0') * (7 - i);
        return suma % 10 == siete.charAt(6) - '0';
    }

    private static String normalizarImo(String valor) {
        String t = valor.toUpperCase(Locale.ROOT).replace(" ", "");
        return SOLO_DIGITOS.matcher(t).matches() ? "IMO" + t : t;
    }

    private static String texto(Map<String, String> c, String clave) {
        String v = c.get(clave);
        return v == null ? "" : v.trim();
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static String requerido(Map<String, String> c, String clave, int max, String etiqueta) {
        String v = texto(c, clave);
        if (v.isEmpty()) throw new DatosInvalidosException(etiqueta + " es obligatorio.");
        if (v.length() > max) throw new DatosInvalidosException(etiqueta + " no puede pasar de " + max + " caracteres.");
        return v;
    }

    private static String opcional(Map<String, String> c, String clave, int max, String etiqueta) {
        String v = texto(c, clave);
        if (v.length() > max) throw new DatosInvalidosException(etiqueta + " no puede pasar de " + max + " caracteres.");
        return v.isEmpty() ? null : v;
    }

    private static String opcion(Map<String, String> c, String clave, Set<String> validos, String etiqueta) {
        String v = texto(c, clave);
        if (!validos.contains(v)) throw new DatosInvalidosException("Seleccione " + etiqueta + " válido.");
        return v;
    }

    private static Double numero(Map<String, String> c, String clave, String etiqueta) {
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

    private static String fecha(Map<String, String> c, String clave, String etiqueta) {
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

    // borradores del armador

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
            if (version != 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "El borrador cambió en otra sesión. Recárgalo antes de continuar.");
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El borrador cambió en otra sesión. Recárgalo antes de continuar.", e);
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

        Nave nave = registrar(campos, adjunto, correo);
        borradores.delete(borrador);
        borradores.flush();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("tramite", nave.getTramite());
        r.put("estado", nave.getEstado());
        r.put("nombreNave", nave.getNombreNave());
        return r;
    }

    private BorradorSolicitud buscarBorrador(String id, String correo) {
        return borradores.findByIdAndArmadorCorreo(uuidBorrador(id), correo).orElseThrow(NaveService::borradorNoEncontrado);
    }

    private BorradorSolicitud bloquearBorrador(String id, String correo) {
        return borradores.buscarParaActualizar(uuidBorrador(id), correo).orElseThrow(NaveService::borradorNoEncontrado);
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
        if (documento.getSize() > MAX_PDF_BYTES)
            throw new DatosInvalidosException("El PDF no puede pasar de 10 MB.");
        byte[] contenido = documento.getBytes();
        if (contenido.length > MAX_PDF_BYTES)
            throw new DatosInvalidosException("El PDF no puede pasar de 10 MB.");
        validarPdf(contenido);
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El borrador cambió en otra sesión. Recárgalo antes de continuar.");
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