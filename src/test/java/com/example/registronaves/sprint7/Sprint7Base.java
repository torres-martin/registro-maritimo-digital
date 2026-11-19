package com.example.registronaves.sprint7;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sprint 7: base de las pruebas backend. Usa la misma base de datos que la aplicación
// (MariaDB 10.4 de XAMPP en local y en el CI), con el esquema creado por Flyway.
@SpringBootTest
@AutoConfigureMockMvc
abstract class Sprint7Base {

    static final String ARMADOR = "naviera@amp.com";
    static final String ARMADOR_2 = "naviera.sprint7@amp.com";
    static final String FUNCIONARIO = "funcionario@amp.com";
    static final String ASEGURADORA = "aseguradora@amp.com";

    static final byte[] PDF = pdfDePrueba();

    // PDF real y legible: el servicio rechaza los archivos que no se pueden abrir
    private static byte[] pdfDePrueba() {
        java.io.ByteArrayOutputStream salida = new java.io.ByteArrayOutputStream();
        com.lowagie.text.Document doc = new com.lowagie.text.Document();
        com.lowagie.text.pdf.PdfWriter.getInstance(doc, salida);
        doc.open();
        doc.add(new com.lowagie.text.Paragraph("Documento base de prueba - Sprint 7"));
        doc.close();
        return salida.toByteArray();
    }

    private static final Random AZAR = new Random();

    @Autowired
    MockMvc mvc;

    static RequestPostProcessor armador() { return user(ARMADOR).roles("ARMADOR"); }
    static RequestPostProcessor armador2() { return user(ARMADOR_2).roles("ARMADOR"); }
    static RequestPostProcessor funcionario() { return user(FUNCIONARIO).roles("FUNCIONARIO"); }
    static RequestPostProcessor aseguradora() { return user(ASEGURADORA).roles("ASEGURADORA"); }

    // IMO con dígito de control válido (DEF-01), distinto en cada llamada
    static String imoValido() {
        int[] pesos = {7, 6, 5, 4, 3, 2};
        StringBuilder sb = new StringBuilder("9");
        int suma = 9 * pesos[0];
        for (int i = 1; i < 6; i++) {
            int d = AZAR.nextInt(10);
            suma += d * pesos[i];
            sb.append(d);
        }
        return sb.append(suma % 10).toString();
    }

    static MockMultipartHttpServletRequestBuilder conCampos(MockMultipartHttpServletRequestBuilder b, String nombreNave, String imo) {
        return b.param("documentoBase", "Venta judicial")
                .param("fechaCelebracion", "2025-01-15")
                .param("nombreTransmitente", "Transmitente de Prueba S.A.")
                .param("propietario", "Naviera de Prueba S.A.")
                .param("nombreNave", nombreNave)
                .param("imo", imo)
                .param("tipoNave", "Granelero")
                .param("solicitante", "Solicitante de Prueba")
                .param("abogado", "Abogado de Prueba");
    }

    static long idDeTramite(String cuerpoJson) {
        String tramite = cuerpoJson.replaceAll("(?s).*\"tramite\":\"([^\"]+)\".*", "$1");
        return Long.parseLong(tramite.substring(tramite.lastIndexOf('-') + 1));
    }

    // Registra una solicitud como armador y devuelve su id
    long registrar(RequestPostProcessor quien, String nombreNave, String imo) throws Exception {
        String cuerpo = mvc.perform(conCampos(multipart("/api/naves")
                        .file(new MockMultipartFile("documento", "base.pdf", "application/pdf", PDF)), nombreNave, imo)
                        .with(quien).with(csrf()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return idDeTramite(cuerpo);
    }

    long registrar(String nombreNave) throws Exception {
        return registrar(armador(), nombreNave, imoValido());
    }

    void decidir(long id, String estado, String observacion) throws Exception {
        mvc.perform(put("/api/naves/" + id + "/estado").with(funcionario()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"" + estado + "\",\"observacion\":\"" + observacion + "\"}"))
                .andExpect(status().isOk());
    }

    void revocar(long id, String motivo) throws Exception {
        mvc.perform(put("/api/naves/" + id + "/revocar").with(funcionario()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"" + motivo + "\"}"))
                .andExpect(status().isOk());
    }
}
