package com.example.registronaves.controller;

import com.example.registronaves.Model.Nave;
import com.example.registronaves.service.BorradorService;
import com.example.registronaves.service.NaveService;
import com.example.registronaves.service.RegistroService;
import com.example.registronaves.service.RevisionService;
import com.example.registronaves.service.RevocacionService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NaveController {

    public record Decision(String estado, String observacion, String liquidacion) {}

    public record Revocacion(String motivo) {}

    private final NaveService servicio;
    private final RegistroService registro;
    private final BorradorService borradores;
    private final RevisionService revision;
    private final RevocacionService revocacion;

    public NaveController(NaveService servicio, RegistroService registro, BorradorService borradores,
                          RevisionService revision, RevocacionService revocacion) {
        this.servicio = servicio;
        this.registro = registro;
        this.borradores = borradores;
        this.revision = revision;
        this.revocacion = revocacion;
    }

    private static boolean esFuncionario(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("ROLE_FUNCIONARIO"));
    }

    // Armador
    @PostMapping("/naves")
    public ResponseEntity<Map<String, Object>> registrar(@RequestParam Map<String, String> campos,
                                                         @RequestParam(value = "documento", required = false) MultipartFile documento,
                                                         Authentication auth) throws IOException {
        Nave n = registro.registrar(campos, documento, auth.getName());
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("tramite", n.getTramite());
        r.put("estado", n.getEstado());
        r.put("nombreNave", n.getNombreNave());
        return ResponseEntity.status(HttpStatus.CREATED).body(r);
    }

    @GetMapping("/naves/mias")
    public List<Map<String, Object>> mias(Authentication auth) {
        return servicio.mias(auth.getName());
    }

    @GetMapping("/naves/mias/vencimientos")
    public List<Map<String, Object>> vencimientosMios(Authentication auth) {
        return servicio.vencimientosMios(auth.getName());
    }

    // Funcionario
    @GetMapping("/naves/todas")
    public List<Map<String, Object>> todas() {
        return servicio.todas();
    }

    @PutMapping("/naves/{id}/estado")
    public Map<String, Object> decidir(@PathVariable("id") Long id, @RequestBody Decision d, Authentication auth) {
        return revision.cambiarEstado(id, d.estado(), d.observacion(), d.liquidacion(), auth.getName());
    }

    @PutMapping("/naves/{id}/revocar")
    public Map<String, Object> revocar(@PathVariable("id") Long id, @RequestBody Revocacion r, Authentication auth) {
        return revocacion.revocar(id, r.motivo(), auth.getName());
    }

    // Aseguradora
    @GetMapping("/naves/consulta")
    public List<Map<String, Object>> consulta() {
        return servicio.consulta();
    }

    // Armador (solo las suyas) y funcionario
    @GetMapping("/naves/{id}")
    public Map<String, Object> detalle(@PathVariable("id") Long id, Authentication auth) {
        return servicio.detalle(id, auth.getName(), esFuncionario(auth));
    }

    @GetMapping("/naves/{id}/documento")
    public ResponseEntity<Resource> documento(@PathVariable("id") Long id, Authentication auth) {
        Path archivo = servicio.rutaDocumento(id, auth.getName(), esFuncionario(auth));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"solicitud-" + id + ".pdf\"")
                .body(new FileSystemResource(archivo));
    }

    @GetMapping("/naves/{id}/certificado")
    public ResponseEntity<byte[]> certificado(@PathVariable("id") Long id, Authentication auth) {
        byte[] pdf = servicio.certificadoPdf(id, auth.getName(), esFuncionario(auth));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"certificado-" + id + ".pdf\"")
                .body(pdf);
    }

    // Público, sin sesión
    @GetMapping("/publico/verificar/{codigo}")
    public Map<String, Object> verificar(@PathVariable("codigo") String codigo) {
        return servicio.verificar(codigo);
    }

    // Borradores del armador

    @GetMapping("/borradores")
    public List<Map<String, Object>> listarBorradores(Authentication auth) {
        return borradores.listarBorradores(auth.getName());
    }

    @GetMapping("/borradores/{id}")
    public Map<String, Object> detalleBorrador(@PathVariable("id") String id, Authentication auth) {
        return borradores.detalleBorrador(id, auth.getName());
    }

    @PutMapping(value = "/borradores/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> guardarBorrador(@PathVariable("id") String id,
                                       @RequestParam("datos") String datos,
                                       @RequestParam("paso") int paso,
                                       @RequestParam("version") long version,
                                       @RequestParam(value = "origenSolicitudId", required = false) Long origenSolicitudId,
                                       @RequestParam(value = "quitarDocumento", defaultValue = "false") boolean quitarDocumento,
                                       @RequestParam(value = "documento", required = false) MultipartFile documento,
                                       Authentication auth) throws IOException {
        return borradores.guardarBorrador(id, datos, paso, version, origenSolicitudId, quitarDocumento, documento, auth.getName());
    }

    @GetMapping("/borradores/{id}/documento")
    public ResponseEntity<byte[]> documentoBorrador(@PathVariable("id") String id, Authentication auth) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"documento-borrador.pdf\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(borradores.documentoBorrador(id, auth.getName()));
    }

    @DeleteMapping("/borradores/{id}")
    public ResponseEntity<Void> eliminarBorrador(@PathVariable("id") String id,
                                        @RequestParam("version") long version, Authentication auth) {
        borradores.eliminarBorrador(id, version, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/borradores/{id}/enviar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> enviarBorrador(@PathVariable("id") String id,
                                                     @RequestParam Map<String, String> campos,
                                                     @RequestParam("version") long version,
                                                     @RequestParam(value = "documento", required = false) MultipartFile documento,
                                                     Authentication auth) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(borradores.enviarBorrador(id, campos, version, documento, auth.getName()));
    }
}