package com.example.registronaves.sprint7;

import com.example.registronaves.Model.Rol;
import com.example.registronaves.Model.Usuario;
import com.example.registronaves.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Year;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// DEF-03: la corrección de una solicitud rechazada queda vinculada a la de origen
class Sprint7CorreccionTest extends Sprint7Base {

    @Autowired
    UsuarioRepository usuarios;

    @BeforeEach
    void segundoArmador() {
        if (usuarios.findByCorreo(ARMADOR_2).isEmpty())
            usuarios.save(new Usuario(ARMADOR_2, "Naviera Sprint 7", "sin-acceso", Rol.ARMADOR));
    }

    private static MockMultipartHttpServletRequestBuilder guardarBorrador(String id, long origen) {
        MockMultipartHttpServletRequestBuilder b = multipart(org.springframework.http.HttpMethod.PUT, "/api/borradores/" + id);
        b.param("datos", "{\"nombreNave\":\"Nave Corregida\"}")
                .param("paso", "1").param("version", "0")
                .param("origenSolicitudId", String.valueOf(origen));
        return b;
    }

    private String crearBorradorDeCorreccion(long origen, RequestPostProcessor quien) throws Exception {
        String id = UUID.randomUUID().toString();
        mvc.perform(guardarBorrador(id, origen).with(quien).with(csrf())).andExpect(status().isOk());
        return id;
    }

    @Test
    @DisplayName("S7-DEF03-01 La corrección enviada queda enlazada al trámite rechazado y a la bitácora")
    void correccionVinculada() throws Exception {
        String imo = imoValido();
        long origen = registrar(armador(), "Nave Rechazada", imo);
        decidir(origen, "Rechazado", "Corregir el nombre del propietario");
        String tramiteOrigen = String.format("TR-%d-%04d", Year.now().getValue(), origen);

        String borrador = crearBorradorDeCorreccion(origen, armador());
        String cuerpo = mvc.perform(conCampos(multipart("/api/borradores/" + borrador + "/enviar")
                                .file(new MockMultipartFile("documento", "base.pdf", "application/pdf", PDF)),
                        "Nave Corregida", imo)
                        .param("version", "0").with(armador()).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tramiteOrigen").value(tramiteOrigen))
                .andExpect(jsonPath("$.estado").value("En revisión"))
                .andReturn().getResponse().getContentAsString();
        long nueva = idDeTramite(cuerpo);

        // El funcionario ve el vínculo en el detalle
        mvc.perform(get("/api/naves/" + nueva).with(funcionario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solicitudOrigenId").value((int) origen))
                .andExpect(jsonPath("$.tramiteOrigen").value(tramiteOrigen));
        // El armador también lo ve en su detalle
        mvc.perform(get("/api/naves/" + nueva).with(armador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tramiteOrigen").value(tramiteOrigen));
        // Y la bitácora registra el reenvío con su origen
        mvc.perform(get("/api/auditoria").param("naveId", String.valueOf(nueva)).with(funcionario()))
                .andExpect(jsonPath("$[?(@.accion == 'Reenviar corrección')].detalle", contains("Corrige " + tramiteOrigen)));
    }

    @Test
    @DisplayName("S7-DEF03-02 No se puede abrir una corrección de una solicitud que no fue rechazada")
    void soloRechazadas() throws Exception {
        long origen = registrar("Nave En Revision");
        mvc.perform(guardarBorrador(UUID.randomUUID().toString(), origen).with(armador()).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Solo se puede corregir una solicitud rechazada."));
    }

    @Test
    @DisplayName("S7-DEF03-03 Un armador no puede corregir la solicitud rechazada de otro")
    void soloSuPropiaSolicitud() throws Exception {
        long origen = registrar("Nave Ajena");
        decidir(origen, "Rechazado", "Documento ilegible");
        mvc.perform(guardarBorrador(UUID.randomUUID().toString(), origen).with(armador2()).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
