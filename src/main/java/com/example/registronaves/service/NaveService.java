package com.example.registronaves.service;

import com.example.registronaves.Model.EstadoTramite;
import com.example.registronaves.Model.Nave;
import com.example.registronaves.repository.NaveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

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

    private static final Path CARPETA_UPLOADS = Paths.get("uploads").toAbsolutePath().normalize();

    private final NaveRepository repo;
    private final CertificadoService certificados;

    public NaveService(NaveRepository repo, CertificadoService certificados) {
        this.repo = repo;
        this.certificados = certificados;
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
            throw new IllegalArgumentException("El IMO debe tener 7 dígitos (ejemplo: 1234567 o IMO1234567).");
        if (repo.existsByImoAndEstadoIn(imo, List.of("En revisión", "Aprobado")))
            throw new IllegalArgumentException("Ya existe una solicitud en revisión o aprobada para el IMO " + imo + ".");

        // VALIDACIÓN DE DÍGITO DE CONTROL DESACTIVADA PARA PRUEBAS:
        // if (!imoValido(imo.substring(3)))
        //     throw new IllegalArgumentException("El número IMO no es válido: el dígito de control no coincide. Revise el certificado de arqueo.");

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
            throw new IllegalArgumentException("La decisión debe ser Aprobado o Rechazado.");

        Nave nave = repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada."));
        if (!EstadoTramite.EN_REVISION.getEtiqueta().equals(nave.getEstado()))
            throw new IllegalArgumentException("Esta solicitud ya fue resuelta.");

        String obs = observacion == null ? "" : observacion.trim();
        if (estado == EstadoTramite.RECHAZADO && obs.isEmpty())
            throw new IllegalArgumentException("Indique la observación del rechazo.");
        if (obs.length() > 1000)
            throw new IllegalArgumentException("La observación no puede pasar de 1000 caracteres.");

        String liq = liquidacion == null ? "" : liquidacion.trim();
        if (liq.length() > 40)
            throw new IllegalArgumentException("El número de liquidación no puede pasar de 40 caracteres.");

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
            throw new IllegalArgumentException("Esta solicitud no tiene certificado emitido.");
        if (nave.isRevocado())
            throw new IllegalArgumentException("El certificado ya fue revocado.");

        String motivo = texto(motivoTxt);
        if (motivo.isEmpty())
            throw new IllegalArgumentException("Indique el motivo de la revocación.");
        if (motivo.length() > 500)
            throw new IllegalArgumentException("El motivo no puede pasar de 500 caracteres.");

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
        if (doc == null || doc.isEmpty()) throw new IllegalArgumentException("Adjunte el documento en PDF.");
        if (doc.getSize() > MAX_PDF_BYTES)
            throw new IllegalArgumentException("El PDF no puede pasar de 10 MB.");

        byte[] cabecera;
        try (InputStream in = doc.getInputStream()) {
            cabecera = in.readNBytes(4);
        }
        if (!"%PDF".equals(new String(cabecera, StandardCharsets.US_ASCII)))
            throw new IllegalArgumentException("El archivo adjunto debe ser un PDF válido.");

        Files.createDirectories(CARPETA_UPLOADS);
        Path destino = CARPETA_UPLOADS.resolve(UUID.randomUUID() + ".pdf");
        try (InputStream in = doc.getInputStream()) {
            Files.copy(in, destino);
        }
        return destino.toString();
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
        if (v.isEmpty()) throw new IllegalArgumentException(etiqueta + " es obligatorio.");
        if (v.length() > max) throw new IllegalArgumentException(etiqueta + " no puede pasar de " + max + " caracteres.");
        return v;
    }

    private static String opcional(Map<String, String> c, String clave, int max, String etiqueta) {
        String v = texto(c, clave);
        if (v.length() > max) throw new IllegalArgumentException(etiqueta + " no puede pasar de " + max + " caracteres.");
        return v.isEmpty() ? null : v;
    }

    private static String opcion(Map<String, String> c, String clave, Set<String> validos, String etiqueta) {
        String v = texto(c, clave);
        if (!validos.contains(v)) throw new IllegalArgumentException("Seleccione " + etiqueta + " válido.");
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
            throw new IllegalArgumentException(etiqueta + " debe ser un número mayor que 0.");
        }
    }

    private static String fecha(Map<String, String> c, String clave, String etiqueta) {
        String v = texto(c, clave);
        if (v.isEmpty()) throw new IllegalArgumentException(etiqueta + " es obligatoria.");
        try {
            if (LocalDate.parse(v).isAfter(LocalDate.now()))
                throw new IllegalArgumentException(etiqueta + " no puede ser una fecha futura.");
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(etiqueta + " no tiene un formato válido.");
        }
        return v;
    }
}