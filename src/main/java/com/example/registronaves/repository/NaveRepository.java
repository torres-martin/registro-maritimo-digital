package com.example.registronaves.repository;

import com.example.registronaves.Model.Nave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;


@Repository
public interface NaveRepository extends JpaRepository<Nave, Long> {
    boolean existsByImo(String imo);
    boolean existsByImoAndEstadoIn(String imo, Collection<String> estados);
    Optional<Nave> findByTramite(String tramite);
    Optional<Nave> findByCodigoVerificacion(String codigoVerificacion);
    List<Nave> findAllByOrderByIdDesc();
    List<Nave> findByArmadorCorreoOrderByIdDesc(String armadorCorreo);

    @Query("""
            select n from Nave n
            where n.armadorCorreo = :correo
              and n.estado = 'Aprobado'
              and n.certificadoFolio is not null
              and trim(n.certificadoFolio) <> ''
              and (n.revocado = false or n.revocado is null)
              and n.fechaVencimiento between :desde and :hasta
            order by n.fechaVencimiento asc, n.id asc
            """)
    List<Nave> findVencimientosPorArmador(@Param("correo") String correo,
                                        @Param("desde") LocalDate desde,
                                        @Param("hasta") LocalDate hasta);
}

