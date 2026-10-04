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
        if (usuarios.count() > 0) return;
        crear("Naviera del Pacífico S.A.", "naviera@amp.com", "1234567", Rol.ARMADOR);
        crear("Aseguradora del Istmo S.A.", "aseguradora@amp.com", "1234567", Rol.ASEGURADORA);
        crear("Funcionario AMP", "funcionario@amp.com", "1234567", Rol.FUNCIONARIO);
    }

    private void crear(String nombre, String correo, String clave, Rol rol) {
        usuarios.save(new Usuario(correo, nombre, encoder.encode(clave), rol));
    }
}