package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioMisiones extends JpaRepository<Mision, UUID> {

    @Query("""
    SELECT m FROM Mision m
    WHERE (:nombreMision IS NULL OR LOWER(m.nombreMision) LIKE :nombreMision)
      AND (:insignia IS NULL OR LOWER(m.insigniaObjetivo.nombre) LIKE :insignia)
      AND (:atributo IS NULL OR m.reglaDeProgreso.atributo = :atributo)
    """)

    Page<Mision> findAllByFiltros(
        @Param("nombreMision") String nombreMision,
        @Param("insignia") String insignia,
        @Param("atributo") AtributoImpacto atributo,
        Pageable pageable
    );

    default Page<Mision> obtenerTodas(String nombreMision, String insigniaObjetivo, String atributoStr, Pageable pageable) {
        String patronNombre = (nombreMision != null && !nombreMision.isBlank())
                              ? "%" + nombreMision.trim().toLowerCase() + "%"
                              : null;

        String patronInsignia = (insigniaObjetivo != null && !insigniaObjetivo.isBlank())
                                ? "%" + insigniaObjetivo.trim().toLowerCase() + "%"
                                : null;

        AtributoImpacto atributo = (atributoStr != null && !atributoStr.isBlank())
                                   ? AtributoImpacto.valueOf(atributoStr.trim().toUpperCase())
                                   : null;

        return this.findAllByFiltros(patronNombre, patronInsignia, atributo, pageable);
    }

    /**
     * Resuelve los ids de misiones del DTO.
     *
     * <p>Se deduplican antes de consultar y se comparan conjuntos, no tamaños de lista.
     * Antes se comparaba {@code misiones.size() != idMisiones.size()}, así que un
     * {@code {"misiones": ["m1", "m1"]}} con m1 existente devolvía "Una o más misiones
     * solicitadas no existen", que es falso: m1 existe, lo que hay es un id repetido
     * (punto 19).
     */
    default List<Mision> conseguirMisiones(List<UUID> idMisiones) {
        if (idMisiones == null || idMisiones.isEmpty()) {
            return List.of();
        }

        List<UUID> idsUnicos = new ArrayList<>(new LinkedHashSet<>(idMisiones));

        List<Mision> misiones = findAllById(idsUnicos);
        if (misiones.size() != idsUnicos.size()) {
            throw new DatosInvalidosException("Una o más misiones solicitadas no existen");
        }
        return misiones;
    }

    default Mision obtenerPorId(UUID id) {
        return findById(id).orElse(null);
    }

}