package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioImpactos;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public record GestorImpacto(RepositorioImpactos repositorio,
                            DonacionClient cliente) {
    public void guardarDonacion(ImpactoDonacion donacion){
        repositorio.guardar(donacion);
    }

    public MedioContacto obtenerContacto(UUID idUsuario){
        return cliente.obtenerContactoPersona(idUsuario);
    }
}
