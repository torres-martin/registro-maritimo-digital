package com.example.registronaves.config;

import com.example.registronaves.Model.Rol;
import com.example.registronaves.Model.Usuario;
import com.example.registronaves.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatosIniciales implements CommandLineRunner {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public DatosIniciales(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        crear("Naviera", "naviera@amp.com", "1234567", Rol.ARMADOR);
        crear("Aseguradora", "aseguradora@amp.com", "1234567", Rol.ASEGURADORA);
        crear("Funcionario", "funcionario@amp.com", "1234567", Rol.FUNCIONARIO);
        // Usuarios que usan las pruebas de Katalon (aislamiento entre armadores y bloqueo por intentos)
        crear("Naviera 2", "naviera2@amp.com", "1234567", Rol.ARMADOR);
        crear("Cuenta de bloqueo", "bloqueo@amp.com", "1234567", Rol.ARMADOR);
    }

    private void crear(String nombre, String correo, String clave, Rol rol) {
        if (usuarios.findByCorreo(correo).isPresent()) return;
        usuarios.save(new Usuario(correo, nombre, encoder.encode(clave), rol));
    }
}