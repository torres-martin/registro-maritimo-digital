package com.example.registronaves.repository;

import com.example.registronaves.Model.Nave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NaveRepository extends JpaRepository<Nave, Long> {
    boolean existsByImo(String imo);
    boolean existsByImoAndEstadoIn(String imo, Collection<String> estados);
    Optional<Nave> findByTramite(String tramite);
    Optional<Nave> findByCodigoVerificacion(String codigoVerificacion);
    List<Nave> findAllByOrderByIdDesc();
    List<Nave> findByArmadorCorreoOrderByIdDesc(String armadorCorreo);
}