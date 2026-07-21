package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class GestorPerfiles {
    private final RepositorioPerfiles repositorio;

    public GestorPerfiles(RepositorioPerfiles repositorio) {
        this.repositorio = repositorio;
    }

    public void verificarProgresos(){
        repositorio.listarTodos().stream()
                .filter(perfil -> perfil.getMisionActual().getReglaDeProgreso().getConstancia() != null)
                .forEach(Perfil::verificarProgresoMision);
    }
}
