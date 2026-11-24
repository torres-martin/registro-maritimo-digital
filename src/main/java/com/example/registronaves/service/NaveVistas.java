package com.example.registronaves.service;

import com.example.registronaves.Model.Nave;

import java.util.LinkedHashMap;
import java.util.Map;

// Sprint 8: vistas (mapas para la API) de una solicitud. Se separaron de NaveService
// para que las usen los servicios de consulta, revisión y revocación.
final class NaveVistas {

    static final String SIN_DOCUMENTO = "sin_documento.pdf";

    private NaveVistas() {
    }

    static Map<String, Object> lista(Nave n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("tramite", n.getTramite());
        m.put("nombreNave", n.getNombreNave());
        m.put("imo", n.getImo());
        m.put("tipoNave", n.getTipoNave());
        m.put("documentoBase", n.getDocumentoBase());
        m.put("propietario", n.getPropietario());
        m.put("estado", n.getEstado());
        m.put("observacion", n.getObservacion());
        m.put("fechaCreacion", String.valueOf(n.getFechaCreacion()));
        return m;
    }

    // DEF-04: en las tablas, una solicitud con certificado revocado se muestra como "Revocado".
    static String estadoVisible(Nave n) {
        return n.isRevocado() ? "Revocado" : n.getEstado();
    }

    static Map<String, Object> fila(Nave n) {
        Map<String, Object> m = lista(n);
        m.put("estado", estadoVisible(n));
        m.put("tramiteOrigen", n.getTramiteOrigen());
        return m;
    }

    static Map<String, Object> consulta(Nave n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tramite", n.getTramite());
        m.put("nombreNave", n.getNombreNave());
        m.put("imo", n.getImo());
        m.put("tipoNave", n.getTipoNave());
        m.put("propietario", n.getPropietario());
        m.put("estado", estadoVisible(n));
        m.put("estadoCertificado", n.getCertificadoFolio() == null ? null : CertificadoService.estado(n));
        m.put("fechaVencimiento", n.getFechaVencimiento() == null ? null : n.getFechaVencimiento().toString());
        return m;
    }

    static Map<String, Object> detalle(Nave n) {
        Map<String, Object> m = lista(n);
        m.put("liquidacion", n.getLiquidacion());
        m.put("tipoTransmitente", n.getTipoTransmitente());
        m.put("nombreTransmitente", n.getNombreTransmitente());
        m.put("domicilioTransmitente", n.getDomicilioTransmitente());
        m.put("tipoAdquirente", n.getTipoAdquirente());
        m.put("domicilioPropietario", n.getDomicilioPropietario());
        m.put("distintivoLlamada", n.getDistintivoLlamada());
        m.put("patenteNavegacion", n.getPatenteNavegacion());
        m.put("arqueoBruto", n.getArqueoBruto());
        m.put("arqueoNeto", n.getArqueoNeto());
        m.put("eslora", n.getEslora());
        m.put("manga", n.getManga());
        m.put("puntal", n.getPuntal());
        m.put("precioVenta", n.getPrecioVenta());
        m.put("fechaCelebracion", n.getFechaCelebracion());
        m.put("tipoLegalizacion", n.getTipoLegalizacion());
        m.put("solicitante", n.getSolicitante());
        m.put("abogado", n.getAbogado());
        m.put("adicionales", n.getAdicionales());
        m.put("tieneDocumento", n.getRutaDocumentoPdf() != null && !SIN_DOCUMENTO.equals(n.getRutaDocumentoPdf()));

        m.put("certificadoFolio", n.getCertificadoFolio());
        m.put("fechaEmision", n.getFechaEmision() == null ? null : n.getFechaEmision().toString());
        m.put("fechaVencimiento", n.getFechaVencimiento() == null ? null : n.getFechaVencimiento().toString());
        m.put("estadoCertificado", n.getCertificadoFolio() == null ? null : CertificadoService.estado(n));
        m.put("fechaDecision", n.getFechaDecision() == null ? null : n.getFechaDecision().toString());
        m.put("codigoVerificacion", n.getCodigoVerificacion());
        m.put("revocado", n.isRevocado());
        m.put("motivoRevocacion", n.getMotivoRevocacion());
        m.put("fechaRevocacion", n.getFechaRevocacion() == null ? null : n.getFechaRevocacion().toString());
        m.put("revocadoPor", n.getRevocadoPor());
        m.put("solicitudOrigenId", n.getSolicitudOrigenId());
        m.put("tramiteOrigen", n.getTramiteOrigen());

        return m;
    }
}
