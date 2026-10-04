package com.example.registronaves.Model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "naves")
public class Nave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String tramite;

    // I. Documento base
    @Column(name = "documento_base")
    private String documentoBase;

    // II. Transmitente
    @Column(name = "tipo_transmitente")
    private String tipoTransmitente;

    @Column(name = "nombre_transmitente")
    private String nombreTransmitente;

    @Column(name = "domicilio_transmitente")
    private String domicilioTransmitente;

    // II. Adquirente
    @Column(name = "tipo_adquirente")
    private String tipoAdquirente;

    @Column(name = "propietario", nullable = false)
    private String propietario;

    @Column(name = "domicilio_propietario")
    private String domicilioPropietario;

    // III. Nave
    @Column(name = "nombre_nave", nullable = false)
    private String nombreNave;

    @Column(length = 20)
    private String imo;

    @Column(name = "distintivo_llamada")
    private String distintivoLlamada;

    @Column(name = "patente_navegacion")
    private String patenteNavegacion;

    @Column(name = "arqueo_bruto")
    private Double arqueoBruto;

    @Column(name = "arqueo_neto")
    private Double arqueoNeto;

    private Double eslora;
    private Double manga;
    private Double puntal;

    @Column(name = "precio_venta")
    private Double precioVenta;

    @Column(name = "anio_construccion")
    private Integer anioConstruccion;

    @Column(name = "tipo_nave")
    private String tipoNave;

    // IV. Autenticación
    @Column(name = "fecha_celebracion")
    private String fechaCelebracion;

    @Column(name = "tipo_legalizacion")
    private String tipoLegalizacion;

    // Solicitante y abogado
    @Column(name = "solicitante")
    private String solicitante;

    @Column(name = "abogado")
    private String abogado;

    // Resto de campos del F-270 (representantes, fichas, autenticación...) en formato JSON
    @Column(name = "adicionales", columnDefinition = "TEXT")
    private String adicionales;

    // Control
    @Column(name = "estado")
    private String estado = "En revisión";

    @Column(name = "observacion", length = 1000)
    private String observacion;

    @Column(name = "liquidacion", length = 40)
    private String liquidacion;

    @Column(name = "armador_correo", length = 120)
    private String armadorCorreo;

    @Column(name = "ruta_documento_pdf")
    private String rutaDocumentoPdf;

    @Column(name = "fecha_creacion")
    private LocalDate fechaCreacion = LocalDate.now();

    @Column(name = "certificado_folio", unique = true, length = 30)
    private String certificadoFolio;

    @Column(name = "codigo_verificacion", unique = true, length = 20)
    private String codigoVerificacion;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    public Nave() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTramite() { return tramite; }
    public void setTramite(String tramite) { this.tramite = tramite; }

    public String getDocumentoBase() { return documentoBase; }
    public void setDocumentoBase(String documentoBase) { this.documentoBase = documentoBase; }

    public String getTipoTransmitente() { return tipoTransmitente; }
    public void setTipoTransmitente(String tipoTransmitente) { this.tipoTransmitente = tipoTransmitente; }

    public String getNombreTransmitente() { return nombreTransmitente; }
    public void setNombreTransmitente(String nombreTransmitente) { this.nombreTransmitente = nombreTransmitente; }

    public String getDomicilioTransmitente() { return domicilioTransmitente; }
    public void setDomicilioTransmitente(String domicilioTransmitente) { this.domicilioTransmitente = domicilioTransmitente; }

    public String getTipoAdquirente() { return tipoAdquirente; }
    public void setTipoAdquirente(String tipoAdquirente) { this.tipoAdquirente = tipoAdquirente; }

    public String getPropietario() { return propietario; }
    public void setPropietario(String propietario) { this.propietario = propietario; }

    public String getDomicilioPropietario() { return domicilioPropietario; }
    public void setDomicilioPropietario(String domicilioPropietario) { this.domicilioPropietario = domicilioPropietario; }

    public String getNombreNave() { return nombreNave; }
    public void setNombreNave(String nombreNave) { this.nombreNave = nombreNave; }

    public String getImo() { return imo; }
    public void setImo(String imo) { this.imo = imo; }

    public String getDistintivoLlamada() { return distintivoLlamada; }
    public void setDistintivoLlamada(String distintivoLlamada) { this.distintivoLlamada = distintivoLlamada; }

    public String getPatenteNavegacion() { return patenteNavegacion; }
    public void setPatenteNavegacion(String patenteNavegacion) { this.patenteNavegacion = patenteNavegacion; }

    public Double getArqueoBruto() { return arqueoBruto; }
    public void setArqueoBruto(Double arqueoBruto) { this.arqueoBruto = arqueoBruto; }

    public Double getArqueoNeto() { return arqueoNeto; }
    public void setArqueoNeto(Double arqueoNeto) { this.arqueoNeto = arqueoNeto; }

    public Double getEslora() { return eslora; }
    public void setEslora(Double eslora) { this.eslora = eslora; }

    public Double getManga() { return manga; }
    public void setManga(Double manga) { this.manga = manga; }

    public Double getPuntal() { return puntal; }
    public void setPuntal(Double puntal) { this.puntal = puntal; }

    public Double getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(Double precioVenta) { this.precioVenta = precioVenta; }

    public Integer getAnioConstruccion() { return anioConstruccion; }
    public void setAnioConstruccion(Integer anioConstruccion) { this.anioConstruccion = anioConstruccion; }

    public String getTipoNave() { return tipoNave; }
    public void setTipoNave(String tipoNave) { this.tipoNave = tipoNave; }

    public String getFechaCelebracion() { return fechaCelebracion; }
    public void setFechaCelebracion(String fechaCelebracion) { this.fechaCelebracion = fechaCelebracion; }

    public String getTipoLegalizacion() { return tipoLegalizacion; }
    public void setTipoLegalizacion(String tipoLegalizacion) { this.tipoLegalizacion = tipoLegalizacion; }

    public String getSolicitante() { return solicitante; }
    public void setSolicitante(String solicitante) { this.solicitante = solicitante; }

    public String getAbogado() { return abogado; }
    public void setAbogado(String abogado) { this.abogado = abogado; }

    public String getAdicionales() { return adicionales; }
    public void setAdicionales(String adicionales) { this.adicionales = adicionales; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getLiquidacion() { return liquidacion; }
    public void setLiquidacion(String liquidacion) { this.liquidacion = liquidacion; }

    public String getArmadorCorreo() { return armadorCorreo; }
    public void setArmadorCorreo(String armadorCorreo) { this.armadorCorreo = armadorCorreo; }

    public String getRutaDocumentoPdf() { return rutaDocumentoPdf; }
    public void setRutaDocumentoPdf(String rutaDocumentoPdf) { this.rutaDocumentoPdf = rutaDocumentoPdf; }

    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDate fechaCreacion) { this.fechaCreacion = fechaCreacion; }

        public String getCertificadoFolio() { return certificadoFolio; }
    public void setCertificadoFolio(String certificadoFolio) { this.certificadoFolio = certificadoFolio; }

    public String getCodigoVerificacion() { return codigoVerificacion; }
    public void setCodigoVerificacion(String codigoVerificacion) { this.codigoVerificacion = codigoVerificacion; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
}