package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Donacion;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioDonaciones;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class DonacionService {
    private final RepositorioDonaciones repositorioDonaciones = RepositorioDonaciones.getInstance();

    public DonacionDTO obtenerDonacion(UUID id) {
        Donacion donacion = repositorioDonaciones.buscarPorIDDonacion(id);

        if (donacion == null) {
            return null;
        }

        DonacionDTO dto = new DonacionDTO();
        dto.setNombreUsuario(Donacion.getNombreUsuario());
        if (perfil.getCategoriaActual() != null && perfil.getCategoriaActual().getNombre() != null) {
            dto.setCategoria(perfil.getCategoriaActual().getNombre().name());
        } else {
            dto.setCategoria(null);
        }

        return dto;
    }
}
