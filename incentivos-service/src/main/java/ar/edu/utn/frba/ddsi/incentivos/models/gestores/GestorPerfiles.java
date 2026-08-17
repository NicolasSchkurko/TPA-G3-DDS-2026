package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;

import java.util.List;
import java.util.UUID;

public record GestorPerfiles(RepositorioPerfiles repositorio) {

    public void verificarProgresos() {
        repositorio.listarTodos().stream()
                .filter(perfil -> perfil.getMisionActual().getReglaDeProgreso().getConstancia() != null)
                .forEach(Perfil::verificarProgresoMision);
    }

    public Perfil crearPerfil(Perfil nuevo){
        if (repositorio.buscarPorIDUsuario(nuevo.getIdUsuario()) != null) {
            return null;
        }

        repositorio.agregarPerfil(nuevo);

        return repositorio.buscarPorIDPerfil(nuevo.getIdPerfil());
    }

    public List<Boolean> progresarPerfil(UUID idUsuario, ImpactoDonacion donacion){
        Perfil perfil = repositorio.buscarPorIDUsuario(idUsuario);

        if (perfil == null) {
            return null;
        }

        //este metodo actualiza mision, posRanking e insignias,
        //avisa qué se actualizo y si hay que actualizar categoria
        List<Boolean> resultados = perfil.progresarMision(donacion);

        repositorio.actualizar(perfil);

        return resultados;
    }

    public Perfil conseguirPerfil(UUID idUsuario){
        return repositorio.buscarPorIDUsuario(idUsuario);
    }

    public Perfil actualizarPerfil(Perfil perfil){
        return repositorio.actualizar(perfil);
    }
}
