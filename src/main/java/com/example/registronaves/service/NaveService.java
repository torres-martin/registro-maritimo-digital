package com.example.registronaves.service;

import com.example.registronaves.Model.Nave;
import com.example.registronaves.repository.NaveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

// Consultas de solicitudes y certificados.
// Sprint 8: el registro, los borradores, la revisión y la revocación se separaron en
// RegistroService, BorradorService, RevisionService y RevocacionService.
@Service
public class NaveService {

    private final NaveRepository repo;
    private final CertificadoService certificados;

    public NaveService(NaveRepository repo, CertificadoService certificados) {
        this.repo = repo;
        this.certificados = certificados;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> mias(String correo) {
        return repo.findByArmadorCorreoOrderByIdDesc(correo).stream().map(NaveVistas::fila).toList();
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
        return repo.findAllByOrderByIdDesc().stream().map(NaveVistas::fila).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> consulta() {
        return repo.findAllByOrderByIdDesc().stream().map(NaveVistas::consulta).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detalle(Long id, String correo, boolean funcionario) {
        return NaveVistas.detalle(buscar(id, correo, funcionario));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> verificar(String codigo) {
        Nave n = repo.findByCodigoVerificacion(ValidacionSolicitud.texto(codigo).toUpperCase(Locale.ROOT))
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
        if (ruta == null || NaveVistas.SIN_DOCUMENTO.equals(ruta))
            throw new NoSuchElementException("Esta solicitud no tiene documento adjunto.");
        Path archivo = Paths.get(ruta).toAbsolutePath().normalize();
        if (!archivo.startsWith(RegistroService.CARPETA_UPLOADS) || !Files.isReadable(archivo))
            throw new NoSuchElementException("El documento no está disponible.");
        return archivo;
    }

    @Transactional(readOnly = true)
    public byte[] certificadoPdf(Long id, String correo, boolean funcionario) {
        Nave nave = buscar(id, correo, funcionario);
        if (nave.getCertificadoFolio() == null)
            throw new NoSuchElementException("Esta solicitud todavía no tiene certificado.");
        return certificados.pdf(nave);
    }

    private Nave buscar(Long id, String correo, boolean funcionario) {
        Nave n = repo.findById(id).orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada."));
        if (!funcionario && (correo == null || !correo.equals(n.getArmadorCorreo())))
            throw new NoSuchElementException("Solicitud no encontrada.");
        return n;
    }
}
