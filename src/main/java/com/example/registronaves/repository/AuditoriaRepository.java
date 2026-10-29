package com.example.registronaves.repository;

import com.example.registronaves.Model.RegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {
    List<RegistroAuditoria> findAllByOrderByFechaHoraDescIdDesc();
    List<RegistroAuditoria> findByNaveIdOrderByFechaHoraDescIdDesc(Long naveId);
}
