package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Consultas sobre las misiones. {@code conseguirMisiones} garantiza el orden que pidió el
 * admin, porque {@code findAllById} no lo preserva.
 */
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

    default Page<Mision> obtenerTodas(
        String nombreMision,
        String insigniaObjetivo,
        String atributoStr,
        Pageable pageable
    ) {
        String patronNombre = (nombreMision != null && !nombreMision.isBlank())
                              ? "%" + nombreMision.trim().toLowerCase() + "%"
                              : null;

        String patronInsignia = (insigniaObjetivo != null && !insigniaObjetivo.isBlank())
                                ? "%" + insigniaObjetivo.trim().toLowerCase() + "%"
                                : null;

        AtributoImpacto atributo = null;

        if (atributoStr != null && !atributoStr.isBlank()) {
            // Se reusa el normalizador de MisionFactory para aceptar acentos igual que el POST.
            try {
                atributo = AtributoImpacto.valueOf(MisionFactory.normalizar(atributoStr));
            } catch (IllegalArgumentException sinNormalizar) {
                // Se traduce a DatosInvalidosException con la lista de valores aceptados.
                throw new DatosInvalidosException(
                        "'" + atributoStr + "' no es un atributo de impacto válido. Se aceptan: "
                                + List.of(AtributoImpacto.values()));
            }
        }

        return this.findAllByFiltros(patronNombre, patronInsignia, atributo, pageable);
    }

    /**
     * Resuelve los ids de misiones del DTO: deduplica y devuelve en el orden que pidió el
     * admin, que es la secuencia de progresión del donante. Lanza si falta alguna.
     */
    default List<Mision> conseguirMisiones(List<UUID> idMisiones) {
        if (idMisiones == null || idMisiones.isEmpty()) {
            return List.of();
        }

        List<UUID> idsUnicos = new ArrayList<>(new LinkedHashSet<>(idMisiones));

        Map<UUID, Mision> porId = findAllById(idsUnicos).stream()
                .collect(Collectors.toMap(Mision::getIdMision, mision -> mision));

        if (porId.size() != idsUnicos.size()) {
            throw new DatosInvalidosException("Una o más misiones solicitadas no existen");
        }

        return idsUnicos.stream()
                        .map(porId::get)
                        .toList();
    }

    default Mision obtenerPorId(UUID id) {
        return findById(id).orElse(null);
    }

}
