package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.dto.CategoriaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.SecuenciaCategoriasDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioMisiones;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class GestorCategoria {
    //gestiona la secuencia de categorias existentes en el repositorio
    private final RepositorioCategorias repositorio;
    private final RepositorioMisiones repositorioMisiones;

    public GestorCategoria(RepositorioCategorias repositorio,
                           RepositorioMisiones repositorioMisiones) {
        this.repositorio = repositorio;
        this.repositorioMisiones = repositorioMisiones;
    }

    public SecuenciaCategoriasDTO crearCategoria(CategoriaDTO nuevaCategoria) {
        List<Mision> misiones = new ArrayList<>();
        List<String> misionesNuevaCategoria = nuevaCategoria.getMisiones();

        for(String mision : misionesNuevaCategoria){
            List<Mision> lstMisiones = repositorioMisiones.obtenerTodas();
            for(Mision m : lstMisiones){
                if(m.getNombreMision().equals(mision)){
                    misiones.add(m);
                }
            }
        }

        Categoria nueva = new Categoria(nuevaCategoria.getNombre(),
                nuevaCategoria.getPosicionSecuencia(),
                misiones
        );

        repositorio.obtenerDesdeNivel(nueva.getPosicionSecuencia())
                .forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() + 1));

        repositorio.agregarCategoria(nueva);

        //para retornar al admin
        return new SecuenciaCategoriasDTO(repositorio.obtenerCategoriasOrdenadasPor(Categoria::getPosicionSecuencia));
    }

    public SecuenciaCategoriasDTO eliminarCategoria(UUID idCategoria) {
        Categoria cat = repositorio.buscarPorId(idCategoria);
        repositorio.eliminarCategoria(cat);
        repositorio.obtenerDesdeNivel(cat.getPosicionSecuencia() + 1)
                .forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() - 1));

        //para retornar al admin
        return new SecuenciaCategoriasDTO(repositorio.obtenerCategoriasOrdenadasPor(Categoria::getPosicionSecuencia));
    }

    //colocar aca tmb para q el admin pueda gestionar el repo de misiones
}
