package com.example.registronaves.sprint8;

import com.example.registronaves.service.BorradorService;
import com.example.registronaves.service.NaveService;
import com.example.registronaves.service.RegistroService;
import com.example.registronaves.service.RevisionService;
import com.example.registronaves.service.RevocacionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sprint 8: NaveService se dividió en registro, borradores, revisión y revocación.
// Estas pruebas recorren el ciclo completo a través de los servicios nuevos.
@SpringBootTest
@AutoConfigureMockMvc
class Sprint8DivisionNaveServiceTest {

    private static final String ARMADOR = "naviera@amp.com";
    private static final Random AZAR = new Random();
    private static final byte[] PDF = pdfDePrueba();

    @Autowired
    MockMvc mvc;

    @Autowired
    RegistroService registro;
    @Autowired
    BorradorService borradores;
    @Autowired
    RevisionService revision;
    @Autowired
    RevocacionService revocacion;

    @Test
    @DisplayName("S8-DIV-01: registrar, aprobar, verificar y revocar funcionan con los servicios separados")
    void cicloCompleto() throws Exception {
        String cuerpo = mvc.perform(conCampos(multipart("/api/naves")
                                .file(new MockMultipartFile("documento", "base.pdf", "application/pdf", PDF)),
                        "Nave Division S8", imoValido())
                        .with(armador()).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("En revisión"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long id = idDeTramite(cuerpo);

        String aprobada = mvc.perform(put("/api/naves/" + id + "/estado").with(funcionario()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"Aprobado\",\"observacion\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Aprobado"))
                .andExpect(jsonPath("$.estadoCertificado").value("Vigente"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String codigo = aprobada.replaceAll("(?s).*\"codigoVerificacion\":\"([^\"]+)\".*", "$1");

        mvc.perform(get("/api/publico/verificar/" + codigo).with(otraIp()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Vigente"));

        mvc.perform(put("/api/naves/" + id + "/revocar").with(funcionario()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Prueba de la división de NaveService\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revocado").value(true));

        mvc.perform(get("/api/publico/verificar/" + codigo).with(otraIp()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Revocado"));

        mvc.perform(get("/api/naves/" + id).with(armador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Aprobado"))
                .andExpect(jsonPath("$.revocado").value(true));
    }

    @Test
    @DisplayName("S8-DIV-02: un borrador con PDF se envía como solicitud a través de RegistroService")
    void borradorSeEnviaComoSolicitud() throws Exception {
        String borrador = UUID.randomUUID().toString();
        MockMultipartHttpServletRequestBuilder guardar = multipart(HttpMethod.PUT, "/api/borradores/" + borrador);
        guardar.file(new MockMultipartFile("documento", "base.pdf", "application/pdf", PDF))
                .param("datos", "{\"nombreNave\":\"Nave Borrador S8\"}")
                .param("paso", "3").param("version", "0");
        mvc.perform(guardar.with(armador()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneDocumento").value(true));

        // Sin adjuntar de nuevo el PDF: se usa el que guardó el borrador
        mvc.perform(conCampos(multipart("/api/borradores/" + borrador + "/enviar"), "Nave Borrador S8", imoValido())
                        .param("version", "0").with(armador()).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("En revisión"))
                .andExpect(jsonPath("$.nombreNave").value("Nave Borrador S8"));

        mvc.perform(get("/api/borradores/" + borrador).with(armador()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("S8-DIV-03: NaveService solo conserva las consultas")
    void naveServiceSoloConsulta() {
        Set<String> publicos = Arrays.stream(NaveService.class.getDeclaredMethods())
                .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                .map(Method::getName)
                .collect(Collectors.toSet());
        assertThat(publicos).containsExactlyInAnyOrder(
                "mias", "vencimientosMios", "todas", "consulta", "detalle", "verificar", "rutaDocumento", "certificadoPdf");
        assertThat(registro).isNotNull();
        assertThat(borradores).isNotNull();
        assertThat(revision).isNotNull();
        assertThat(revocacion).isNotNull();
    }

    private static RequestPostProcessor armador() {
        return user(ARMADOR).roles("ARMADOR");
    }

    private static RequestPostProcessor funcionario() {
        return user("funcionario@amp.com").roles("FUNCIONARIO");
    }

    // La verificación pública tiene límite por IP: estas consultas usan una IP propia.
    private static RequestPostProcessor otraIp() {
        return peticion -> {
            peticion.setRemoteAddr("10.8.8." + (1 + AZAR.nextInt(250)));
            return peticion;
        };
    }

    private static MockMultipartHttpServletRequestBuilder conCampos(MockMultipartHttpServletRequestBuilder b,
                                                                     String nombreNave, String imo) {
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

    private static long idDeTramite(String cuerpoJson) {
        String tramite = cuerpoJson.replaceAll("(?s).*\"tramite\":\"([^\"]+)\".*", "$1");
        return Long.parseLong(tramite.substring(tramite.lastIndexOf('-') + 1));
    }

    // IMO con dígito de control válido (DEF-01), distinto en cada llamada
    private static String imoValido() {
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

    private static byte[] pdfDePrueba() {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        com.lowagie.text.Document doc = new com.lowagie.text.Document();
        com.lowagie.text.pdf.PdfWriter.getInstance(doc, salida);
        doc.open();
        doc.add(new com.lowagie.text.Paragraph("Documento base de prueba - Sprint 8"));
        doc.close();
        return salida.toByteArray();
    }
}
