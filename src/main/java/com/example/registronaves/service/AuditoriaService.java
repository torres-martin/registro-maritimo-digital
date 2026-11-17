package com.example.registronaves.service;

import com.example.registronaves.Model.Nave;
import com.example.registronaves.Model.RegistroAuditoria;
import com.example.registronaves.repository.AuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// HU-21: registra quién hizo qué y cuándo. Solo agrega y consulta; nunca edita ni borra.
@Service
public class AuditoriaService {

    public static final String ENVIAR = "Enviar solicitud";
    public static final String CORREGIR = "Reenviar corrección";
    public static final String APROBAR = "Aprobar";
    public static final String RECHAZAR = "Rechazar";
    public static final String REVOCAR = "Revocar";

    private final AuditoriaRepository repo;

    public AuditoriaService(AuditoriaRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void registrar(String usuario, String accion, Nave nave, String detalle) {
        String quien = usuario == null || usuario.isBlank() ? "sistema" : usuario;
        String texto = detalle == null ? null : (detalle.length() > 1000 ? detalle.substring(0, 1000) : detalle);
        repo.save(new RegistroAuditoria(quien, accion, nave, texto));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listar(Long naveId) {
        List<RegistroAuditoria> filas = naveId == null
                ? repo.findAllByOrderByFechaHoraDescIdDesc()
                : repo.findByNaveIdOrderByFechaHoraDescIdDesc(naveId);
        return filas.stream().map(AuditoriaService::vista).toList();
    }

    private static Map<String, Object> vista(RegistroAuditoria r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("fechaHora", r.getFechaHora().withNano(0).toString());
        m.put("usuario", r.getUsuario());
        m.put("accion", r.getAccion());
        m.put("naveId", r.getNaveId());
        m.put("tramite", r.getTramite());
        m.put("nombreNave", r.getNombreNave());
        m.put("detalle", r.getDetalle());
        return m;
    }
}
