package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.CategoriaBaseInexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.PerfilExistenteException;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PerfilService {
    private final RepositorioPerfiles repositorioPerfiles;
    private final RepositorioCategorias repositorioCategorias;

    public PerfilService(RepositorioPerfiles repositorio,
                         RepositorioCategorias repositorioCategorias) {
        this.repositorioPerfiles = repositorio;
        this.repositorioCategorias = repositorioCategorias;
    }

    public PerfilDTO crearPerfil(PerfilDonanteDTO dto) {
        Perfil nuevo = new Perfil(dto.getIdUsuario(), dto.getNombreUsuario());

        // Buscamos la categoría base ordenando por posición. Si no hay ninguna, arroja error.
        Categoria categoriaBase = repositorioCategorias.findAllByOrderByPosicionSecuenciaAsc().stream()
                                                       .findFirst()
                                                       .orElseThrow(() -> new CategoriaBaseInexistenteException(
                                                           "No existe la categoría base configurada"));

        nuevo.setCategoriaActual(categoriaBase);

        if (categoriaBase.primeraMision() != null) {
            nuevo.setProgresoMisionActual(new ProgresoMision(categoriaBase.primeraMision()));
        }

        if (repositorioPerfiles.existsByIdUsuario(nuevo.getIdUsuario())) {
            throw new PerfilExistenteException(nuevo.getIdUsuario());
        }

        nuevo = repositorioPerfiles.save(nuevo);

        return new PerfilDTO(
            nuevo.getNombreUsuario(),
            nuevo.getCategoriaActual() != null ? nuevo.getCategoriaActual().getNombre() : null,
            nuevo.getInsigniasObtenidas() != null ? nuevo.getInsigniasObtenidas().stream().map(io -> io.getInsignia().getNombre()).toList() : List.of(),
            nuevo.getProgresoMisionActual() != null ? nuevo.getProgresoMisionActual().getMision().getNombreMision() : null,
            null // El puesto ya no se calcula en tiempo real, sino en el cierre de mes
        );
    }

    public PerfilDTO buscarPorIdUsuario(UUID idUsuario){
        // Usamos findByIdUsuario de JpaRepository
        Perfil p = repositorioPerfiles.findByIdUsuario(idUsuario).orElse(null);
        if (p == null) {
            return null;
        }

        return new PerfilDTO(
            p.getNombreUsuario(),
            p.getCategoriaActual() == null ? null : p.getCategoriaActual().getNombre(),
            p.getInsigniasObtenidas() == null ? List.of() : p.getInsigniasObtenidas().stream().map(io -> io.getInsignia().getNombre()).toList(),
            p.getProgresoMisionActual() == null ? null : p.getProgresoMisionActual().getMision().getNombreMision(),
            null); // Ajustar si tienes lógica de Roles implementada en otro lado
    }
}