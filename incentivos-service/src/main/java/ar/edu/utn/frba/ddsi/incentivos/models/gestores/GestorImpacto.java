package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioImpactos;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public record GestorImpacto(RepositorioImpactos repositorio) {
    public void guardarDonacion(ImpactoDonacion donacion){
        repositorio.guardar(donacion);
    }
}
