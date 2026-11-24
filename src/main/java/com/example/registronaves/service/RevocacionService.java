package com.example.registronaves.service;

import com.example.registronaves.Model.Nave;
import com.example.registronaves.exception.DatosInvalidosException;
import com.example.registronaves.repository.NaveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;

// Sprint 8: revocación del certificado por el funcionario (antes en NaveService).
@Service
public class RevocacionService {

    private final NaveRepository repo;
    private final AuditoriaService auditoria;

    public RevocacionService(NaveRepository repo, AuditoriaService auditoria) {
        this.repo = repo;
        this.auditoria = auditoria;
    }

    @Transactional
    public Map<String, Object> revocar(Long id, String motivoTxt, String usuario) {
        Nave nave = repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada."));
        if (nave.getCertificadoFolio() == null)
            throw new DatosInvalidosException("Esta solicitud no tiene certificado emitido.");
        if (nave.isRevocado())
            throw new DatosInvalidosException("El certificado ya fue revocado.");

        String motivo = ValidacionSolicitud.texto(motivoTxt);
        if (motivo.isEmpty())
            throw new DatosInvalidosException("Indique el motivo de la revocación.");
        if (motivo.length() > 500)
            throw new DatosInvalidosException("El motivo no puede pasar de 500 caracteres.");

        nave.setRevocado(true);
        nave.setMotivoRevocacion(motivo);
        nave.setFechaRevocacion(LocalDate.now());
        nave.setRevocadoPor(usuario);
        nave = repo.save(nave);
        auditoria.registrar(usuario, AuditoriaService.REVOCAR, nave, motivo);
        return NaveVistas.detalle(nave);
    }
}
