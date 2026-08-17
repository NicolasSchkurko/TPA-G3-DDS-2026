package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.gestores.GestorAdministradores;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    private final GestorAdministradores gestorAdministradores;

    public AdminService(GestorAdministradores gestorAdministradores) {
        this.gestorAdministradores = gestorAdministradores;
    }

    public Administrador crearAdministrador(Administrador nuevoAdmin) {
        gestorAdministradores.registrarAdministrador(nuevoAdmin);
        return nuevoAdmin;
    }

    public Administrador actualizarAdmin(UUID id, Administrador datosNuevos) {
        Administrador existente = gestorAdministradores.obtenerAdministrador(id);
        if (existente == null) {
            throw new IllegalArgumentException("No se encontró la persona con ID: " + id);
        }

        existente.setHumano(datosNuevos.getHumano());
        existente.getHumano().setMedioDeContacto(datosNuevos.getMedioDeContacto());
        existente.setNombreAMostrar(datosNuevos.getNombreAMostrar());

        gestorAdministradores.modificarAdministrador(id, existente);
        return existente;
    }

    public void eliminarAdmin(UUID id) {
        gestorAdministradores.darDeBajaAdministrador(id);
    }

    /**
     * Medios de contacto de todos los administradores, para uso del
     * sistema de notificaciones (ej. entregas no recibidas).
     */
    public List<MedioDeContacto> obtenerContactosAdministradores() {
        return gestorAdministradores.listarTodosLosAdministradores().stream()
                                    .map(Administrador::getMedioDeContacto)
                                    .toList();
    }
}