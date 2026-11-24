package com.example.registronaves.service;

import com.example.registronaves.Model.EstadoTramite;
import com.example.registronaves.Model.Nave;
import com.example.registronaves.exception.DatosInvalidosException;
import com.example.registronaves.repository.NaveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;

// Sprint 8: decisión del funcionario (aprobar o rechazar) y emisión del certificado
// (antes en NaveService).
@Service
public class RevisionService {

    private final NaveRepository repo;
    private final AuditoriaService auditoria;

    public RevisionService(NaveRepository repo, AuditoriaService auditoria) {
        this.repo = repo;
        this.auditoria = auditoria;
    }

    @Transactional
    public Map<String, Object> cambiarEstado(Long id, String estadoTxt, String observacion, String liquidacion, String usuario) {
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

        nave = repo.save(nave);
        if (estado == EstadoTramite.APROBADO)
            auditoria.registrar(usuario, AuditoriaService.APROBAR, nave, "Certificado " + nave.getCertificadoFolio());
        else
            auditoria.registrar(usuario, AuditoriaService.RECHAZAR, nave, obs);
        return NaveVistas.detalle(nave);
    }

    private void emitirCertificado(Nave nave) {
        LocalDate hoy = LocalDate.now();
        nave.setCertificadoFolio(String.format("RMD-%d-%06d", hoy.getYear(), nave.getId()));
        nave.setCodigoVerificacion(CertificadoService.nuevoCodigo());
        nave.setFechaEmision(hoy);
        nave.setFechaVencimiento(hoy.plusMonths(6));
    }
}
