package com.example.registronaves.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// HU-21: cada fila es una acción registrada. Las columnas no se actualizan: la bitácora es de solo lectura.
@Entity
@Table(name = "bitacora_auditoria", indexes = {
        @Index(name = "idx_bitacora_nave", columnList = "nave_id"),
        @Index(name = "idx_bitacora_fecha", columnList = "fecha_hora")
})
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false, length = 120, updatable = false)
    private String usuario;

    @Column(nullable = false, length = 40, updatable = false)
    private String accion;

    @Column(name = "nave_id", updatable = false)
    private Long naveId;

    @Column(length = 30, updatable = false)
    private String tramite;

    @Column(name = "nombre_nave", length = 100, updatable = false)
    private String nombreNave;

    @Column(length = 1000, updatable = false)
    private String detalle;

    protected RegistroAuditoria() {}

    public RegistroAuditoria(String usuario, String accion, Nave nave, String detalle) {
        this.fechaHora = LocalDateTime.now();
        this.usuario = usuario;
        this.accion = accion;
        if (nave != null) {
            this.naveId = nave.getId();
            this.tramite = nave.getTramite();
            this.nombreNave = nave.getNombreNave();
        }
        this.detalle = detalle;
    }

    public Long getId() { return id; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getUsuario() { return usuario; }
    public String getAccion() { return accion; }
    public Long getNaveId() { return naveId; }
    public String getTramite() { return tramite; }
    public String getNombreNave() { return nombreNave; }
    public String getDetalle() { return detalle; }
}
