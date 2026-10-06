package com.example.registronaves.service;

import com.example.registronaves.Model.Nave;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class CertificadoService {

    // Sin 0/O/1/I para que el código sea fácil de leer y dictar
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom AZAR = new SecureRandom();
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Color AZUL = new Color(0x14, 0x3A, 0x5E);
    private static final Color GRIS = new Color(0x55, 0x60, 0x6B);
    private static final Color VERDE = new Color(0x1E, 0x7B, 0x3A);
    private static final Color ROJO = new Color(0xB0, 0x2A, 0x2A);

    private final String baseUrl;

    public CertificadoService(@Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /** Código de verificación aleatorio con formato XXXX-XXXX-XXXX (no se puede adivinar). */
    public static String nuevoCodigo() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            if (i > 0 && i % 4 == 0) sb.append('-');
            sb.append(ALFABETO.charAt(AZAR.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

        public static String estado(Nave n) {
        if (n.getFechaVencimiento() == null) return "Sin certificado";
        if (n.isRevocado()) return "Revocado";
        return LocalDate.now().isAfter(n.getFechaVencimiento()) ? "Vencido" : "Vigente";
    }
    
    public byte[] pdf(Nave n) {
        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 40, 40, 34, 40);
            PdfWriter.getInstance(doc, salida).setPageEvent(new Marco());
            doc.open();

            // Encabezado: instituciones a la izquierda, folio y estado a la derecha 
            PdfPTable enc = new PdfPTable(new float[] {68f, 32f});
            enc.setWidthPercentage(100);
            PdfPCell izq = sinBorde();
            izq.addElement(new Paragraph("REPÚBLICA DE PANAMÁ", fuente(FontFactory.HELVETICA_BOLD, 8.5f, GRIS)));
            izq.addElement(new Paragraph("AUTORIDAD MARÍTIMA DE PANAMÁ", fuente(FontFactory.HELVETICA_BOLD, 16, AZUL)));
            izq.addElement(new Paragraph("Dirección General de Registro Público de Propiedad de Naves",
                    fuente(FontFactory.HELVETICA, 9.5f, GRIS)));
            izq.addElement(new Paragraph("Panama Maritime Authority · General Directorate of Public Registry of Ownership of Vessels",
                    fuente(FontFactory.HELVETICA_OBLIQUE, 7.5f, GRIS)));
            enc.addCell(izq);
            PdfPCell der = sinBorde();
            String estado = estado(n);
            Paragraph etiqueta = new Paragraph("Documento electrónico · Electronic document", fuente(FontFactory.HELVETICA, 7.5f, GRIS));
            etiqueta.setAlignment(Element.ALIGN_RIGHT);
            Paragraph folio = new Paragraph("N.º " + n.getCertificadoFolio(), fuente(FontFactory.COURIER_BOLD, 14, AZUL));
            folio.setAlignment(Element.ALIGN_RIGHT);
            Paragraph est = new Paragraph(estado.toUpperCase(),
                    fuente(FontFactory.HELVETICA_BOLD, 12, "Vigente".equals(estado) ? VERDE : ROJO));
            est.setAlignment(Element.ALIGN_RIGHT);
            der.addElement(etiqueta);
            der.addElement(folio);
            der.addElement(est);
            enc.addCell(der);
            doc.add(enc);

            //  Título 
            centrado(doc, "CERTIFICADO DE INSCRIPCIÓN PRELIMINAR DE TÍTULO DE PROPIEDAD DE NAVE",
                    fuente(FontFactory.HELVETICA_BOLD, 15, AZUL), 12, 1);
            centrado(doc, "Certificate of preliminary registration of vessel ownership title",
                    fuente(FontFactory.HELVETICA_OBLIQUE, 9, GRIS), 0, 8);

            // Datos en cuatro columnas (dos campos por fila) 
            PdfPTable datos = new PdfPTable(new float[] {19f, 31f, 19f, 31f});
            datos.setWidthPercentage(100);

            seccion(datos, "DATOS DE LA NAVE · VESSEL INFORMATION");
            List<String[]> nave = new ArrayList<>();
            nave.add(new String[] {"Nombre de la nave", "Vessel name", n.getNombreNave()});
            nave.add(new String[] {"Número IMO", "IMO number", n.getImo()});
            nave.add(new String[] {"Tipo de nave", "Vessel type", n.getTipoNave()});
            if (n.getArqueoBruto() != null)
                nave.add(new String[] {"Tonelaje bruto", "Gross tonnage",
                        String.format(java.util.Locale.US, "%,.2f GT", n.getArqueoBruto())});
            if (lleno(n.getDistintivoLlamada()))
                nave.add(new String[] {"Distintivo de llamada", "Call sign", n.getDistintivoLlamada()});
            if (lleno(n.getPatenteNavegacion()))
                nave.add(new String[] {"Patente de navegación", "Navigation registry", n.getPatenteNavegacion()});
            campos(datos, nave);

            seccion(datos, "PARTES · PARTIES");
            List<String[]> partes = new ArrayList<>();
            partes.add(new String[] {"Transmitente del dominio", "Transferor", n.getNombreTransmitente()});
            partes.add(new String[] {"Comprador o propietario", "Buyer or owner", n.getPropietario()});
            campos(datos, partes);

            seccion(datos, "DOCUMENTO Y TRÁMITE · DOCUMENT AND PROCEDURE");
            List<String[]> tramite = new ArrayList<>();
            tramite.add(new String[] {"Documento base", "Base document", n.getDocumentoBase()});
            if (lleno(n.getFechaCelebracion()))
                tramite.add(new String[] {"Fecha del documento", "Date of the document", n.getFechaCelebracion()});
            if (lleno(n.getTipoLegalizacion()))
                tramite.add(new String[] {"Tipo de legalización", "Type of legalization", n.getTipoLegalizacion()});
            tramite.add(new String[] {"Trámite N.º", "Procedure no.", n.getTramite()});
            if (lleno(n.getLiquidacion()))
                tramite.add(new String[] {"Liquidación N.º", "Liquidation no.", n.getLiquidacion()});
            tramite.add(new String[] {"Derechos de registro y calificación", "Registration and review fees", "B/. 20.00"});
            tramite.add(new String[] {"Abogado de la inscripción definitiva", "Attorney, definitive registration", n.getAbogado()});
            tramite.add(new String[] {"Fecha de emisión", "Date of issue", n.getFechaEmision().format(FECHA)});
            tramite.add(new String[] {"Vigente hasta", "Valid until", n.getFechaVencimiento().format(FECHA)});
            campos(datos, tramite);
            doc.add(datos);

            // Pie: texto a la izquierda, QR y código a la derecha 
            PdfPTable pie = new PdfPTable(new float[] {72f, 28f});
            pie.setWidthPercentage(100);
            pie.setSpacingBefore(8);
            PdfPCell texto = sinBorde();
            texto.setPaddingRight(14);
            Paragraph nota = new Paragraph("Esta inscripción es preliminar y tiene una vigencia de seis (6) meses. Antes de que "
                    + "venza, la escritura pública que contiene el título de propiedad debe presentarse ante la Dirección General "
                    + "de Registro Público de Propiedad de Naves para solicitar la inscripción definitiva.",
                    fuente(FontFactory.HELVETICA, 8.5f, GRIS));
            nota.setAlignment(Element.ALIGN_JUSTIFIED);
            texto.addElement(nota);
            Paragraph firma = new Paragraph("Documento firmado electrónicamente por la Autoridad Marítima de Panamá",
                    fuente(FontFactory.HELVETICA_BOLD, 9, Color.BLACK));
            firma.setSpacingBefore(6);
            texto.addElement(firma);
            texto.addElement(new Paragraph("Para confirmar que este certificado es auténtico, escanee el código QR o ingrese el "
                    + "código de verificación en " + baseUrl + "/verificar.html",
                    fuente(FontFactory.HELVETICA, 8.5f, GRIS)));
            texto.addElement(new Paragraph("Fundamento: Ley 55 de 6 de agosto de 2008 y Decreto Ejecutivo 259 de 31 de marzo de 2011.",
                    fuente(FontFactory.HELVETICA_OBLIQUE, 7.5f, GRIS)));
            pie.addCell(texto);

            PdfPCell cqr = sinBorde();
            cqr.setHorizontalAlignment(Element.ALIGN_CENTER);
            Image qr = Image.getInstance(qrPng(baseUrl + "/verificar.html?codigo=" + n.getCodigoVerificacion()));
            qr.scaleToFit(84, 84);
            qr.setAlignment(Image.ALIGN_CENTER);
            cqr.addElement(qr);
            Paragraph cod = new Paragraph(n.getCodigoVerificacion(), fuente(FontFactory.COURIER_BOLD, 11, AZUL));
            cod.setAlignment(Element.ALIGN_CENTER);
            cqr.addElement(cod);
            Paragraph codEt = new Paragraph("Código de verificación", fuente(FontFactory.HELVETICA, 7.5f, GRIS));
            codEt.setAlignment(Element.ALIGN_CENTER);
            cqr.addElement(codEt);
            pie.addCell(cqr);
            doc.add(pie);

            doc.close();
            return salida.toByteArray();
        } catch (DocumentException | IOException | WriterException e) {
            throw new IllegalStateException("No se pudo generar el certificado.", e);
        }
    }

    /** Marco azul y marca de agua en cada página. */
    private static class Marco extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter w, Document d) {
            PdfContentByte c = w.getDirectContentUnder();
            Rectangle r = d.getPageSize();
            c.saveState();
            c.setColorStroke(AZUL);
            c.setLineWidth(1.2f);
            c.rectangle(20, 20, r.getWidth() - 40, r.getHeight() - 40);
            c.stroke();
            PdfGState g = new PdfGState();
            g.setFillOpacity(0.06f);
            c.setGState(g);
            c.setColorFill(AZUL);
            try {
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
                c.beginText();
                c.setFontAndSize(bf, 58);
                c.showTextAligned(Element.ALIGN_CENTER, "AUTORIDAD MARÍTIMA DE PANAMÁ", r.getWidth() / 2, r.getHeight() / 2 - 20, 18);
                c.endText();
            } catch (Exception e) {
                // si falla la fuente, el certificado se emite sin marca de agua
            }
            c.restoreState();
        }
    }

    // utilidades 

    private static Font fuente(String nombre, float tamano, Color color) {
        return FontFactory.getFont(nombre, tamano, Font.NORMAL, color);
    }

    private static void centrado(Document doc, String texto, Font fuente, float antes, float despues)
            throws DocumentException {
        Paragraph p = new Paragraph(texto, fuente);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(antes);
        p.setSpacingAfter(despues);
        doc.add(p);
    }

    private static boolean lleno(String s) { return s != null && !s.isBlank(); }

    private static PdfPCell sinBorde() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private static void seccion(PdfPTable tabla, String titulo) {
        PdfPCell c = new PdfPCell(new Phrase(titulo, fuente(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE)));
        c.setColspan(4);
        c.setBackgroundColor(AZUL);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(4);
        tabla.addCell(c);
    }

    /** Dibuja los campos de dos en dos; si sobra uno, ocupa el ancho restante. */
    private static void campos(PdfPTable tabla, List<String[]> lista) {
        for (int i = 0; i < lista.size(); i += 2) {
            celdaEtiqueta(tabla, lista.get(i));
            celdaValor(tabla, lista.get(i)[2], i + 1 < lista.size() ? 1 : 3);
            if (i + 1 < lista.size()) {
                celdaEtiqueta(tabla, lista.get(i + 1));
                celdaValor(tabla, lista.get(i + 1)[2], 1);
            }
        }
    }

    private static void celdaEtiqueta(PdfPTable tabla, String[] campo) {
        Phrase etiqueta = new Phrase();
        etiqueta.add(new com.lowagie.text.Chunk(campo[0] + "\n", fuente(FontFactory.HELVETICA, 8.5f, GRIS)));
        etiqueta.add(new com.lowagie.text.Chunk(campo[1], fuente(FontFactory.HELVETICA_OBLIQUE, 6.5f, GRIS)));
        tabla.addCell(estiloCelda(new PdfPCell(etiqueta)));
    }

    private static void celdaValor(PdfPTable tabla, String valor, int columnas) {
        PdfPCell c = new PdfPCell(new Phrase(valor == null || valor.isBlank() ? "—" : valor,
                fuente(FontFactory.HELVETICA_BOLD, 10, Color.BLACK)));
        c.setColspan(columnas);
        tabla.addCell(estiloCelda(c));
    }

    private static PdfPCell estiloCelda(PdfPCell c) {
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(new Color(0xD5, 0xDB, 0xE1));
        c.setPaddingTop(3);
        c.setPaddingBottom(3);
        return c;
    }

    private byte[] qrPng(String contenido) throws WriterException, IOException {
        BitMatrix m = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, 0, 0,
                Map.of(EncodeHintType.MARGIN, 1, EncodeHintType.CHARACTER_SET, "UTF-8"));
        int escala = 10;
        BufferedImage img = new BufferedImage(m.getWidth() * escala, m.getHeight() * escala,
                BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                img.setRGB(x, y, m.get(x / escala, y / escala) ? 0x000000 : 0xFFFFFF);
            }
        }
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(img, "png", png);
        return png.toByteArray();
    }
}