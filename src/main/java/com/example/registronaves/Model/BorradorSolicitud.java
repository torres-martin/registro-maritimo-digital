package com.example.registronaves.Model;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "borradores_solicitud", indexes = {
        @Index(name = "idx_borrador_armador_actualizado", columnList = "armador_correo,actualizado")
})
public class BorradorSolicitud {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Version
    private Long version;

    @Column(name = "armador_correo", length = 120, nullable = false, updatable = false)
    private String armadorCorreo;

    @Column(name = "datos_json", columnDefinition = "TEXT", nullable = false)
    private String datosJson = "{}";

    // Permite listar sin recuperar el JSON ni el documento.
    @Column(name = "nombre_nave", length = 100, nullable = false)
    private String nombreNave = "";

    @Column(nullable = false)
    private int paso = 1;

    @Column(name = "origen_solicitud_id")
    private Long origenSolicitudId;

    @Column(nullable = false)
    private Instant actualizado = Instant.now();

    @Column(name = "nombre_documento", length = 255)
    private String nombreDocumento;

    @Column(name = "tamano_documento")
    private Long tamanoDocumento;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "pdf", columnDefinition = "LONGBLOB")
    private byte[] pdf;

    public BorradorSolicitud() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public String getArmadorCorreo() { return armadorCorreo; }
    public void setArmadorCorreo(String armadorCorreo) { this.armadorCorreo = armadorCorreo; }

    public String getDatosJson() { return datosJson; }
    public void setDatosJson(String datosJson) { this.datosJson = datosJson; }

    public String getNombreNave() { return nombreNave; }
    public void setNombreNave(String nombreNave) { this.nombreNave = nombreNave; }

    public int getPaso() { return paso; }
    public void setPaso(int paso) { this.paso = paso; }

    public Long getOrigenSolicitudId() { return origenSolicitudId; }
    public void setOrigenSolicitudId(Long origenSolicitudId) { this.origenSolicitudId = origenSolicitudId; }

    public Instant getActualizado() { return actualizado; }
    public void setActualizado(Instant actualizado) { this.actualizado = actualizado; }

    public String getNombreDocumento() { return nombreDocumento; }
    public void setNombreDocumento(String nombreDocumento) { this.nombreDocumento = nombreDocumento; }

    public Long getTamanoDocumento() { return tamanoDocumento; }
    public void setTamanoDocumento(Long tamanoDocumento) { this.tamanoDocumento = tamanoDocumento; }

    public byte[] getPdf() { return pdf; }
    public void setPdf(byte[] pdf) { this.pdf = pdf; }
}