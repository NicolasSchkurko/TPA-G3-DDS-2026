package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioCategorias extends JpaRepository<Categoria, UUID> {

    Optional<Categoria> findByIdCategoria(UUID idCategoria);

    // Consulta automática generada por Spring Data para buscar niveles superiores o iguales
    List<Categoria> findByPosicionSecuenciaGreaterThanEqual(Integer nivel);

    // Consulta para desplazar posiciones en rangos específicos
    List<Categoria> findByPosicionSecuenciaBetween(Integer start, Integer end);

    // Obtener las categorías ordenadas (reemplaza a tu lógica manual de ordenamiento)
    List<Categoria> findAllByOrderByPosicionSecuenciaAsc();
}