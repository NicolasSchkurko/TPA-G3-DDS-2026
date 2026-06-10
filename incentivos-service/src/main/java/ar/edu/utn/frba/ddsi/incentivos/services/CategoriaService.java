package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.TipoCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioCategorias;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CategoriaService {

    private final RepositorioCategorias repositorio = RepositorioCategorias.getInstance();

    public Categoria crearCategoria(
            TipoCategoria tipo,
            List<Mision> misiones
    ) {
        TipoCategoria siguienteTipo = tipo.siguiente();

        Categoria siguiente = siguienteTipo == tipo
                ? null
                : repositorio.buscarPorTipo(siguienteTipo);

        Categoria categoria = new Categoria(tipo, misiones);
        categoria.setSiguienteCategoria(siguiente);
        repositorio.guardar(categoria);

        return categoria;
    }
}
