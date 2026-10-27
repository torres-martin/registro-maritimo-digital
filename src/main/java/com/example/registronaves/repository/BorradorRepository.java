package com.example.registronaves.repository;

import com.example.registronaves.Model.BorradorSolicitud;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BorradorRepository
        extends JpaRepository<BorradorSolicitud, String> {

    interface Resumen {
        String getId();
        Long getVersion();
        String getNombreNave();
        int getPaso();
        Instant getActualizado();
        boolean getTieneDocumento();
    }

    Optional<BorradorSolicitud> findByIdAndArmadorCorreo(
            String id,
            String armadorCorreo
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b
            from BorradorSolicitud b
            where b.id = :id
              and b.armadorCorreo = :correo
            """)
    Optional<BorradorSolicitud> buscarParaActualizar(
            @Param("id") String id,
            @Param("correo") String correo
    );

    @Query("""
            select b.id as id,
                   b.version as version,
                   b.nombreNave as nombreNave,
                   b.paso as paso,
                   b.actualizado as actualizado,
                   case
                       when b.tamanoDocumento is not null then true
                       else false
                   end as tieneDocumento
            from BorradorSolicitud b
            where b.armadorCorreo = :correo
            order by b.actualizado desc, b.id asc
            """)
    List<Resumen> listarResumen(
            @Param("correo") String correo
    );
}