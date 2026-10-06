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
 * Consultas sobre las misiones.
 *
 * <p>El método {@code default conseguirMisiones} es lo que garantiza el orden del admin:
 * {@code findAllById} no tiene {@code ORDER BY}, así que la base puede devolver las ids en
 * el orden que quiera y el donante arrancaría en una misión distinta de la primera del
 * programa (punto 27).
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
            // Se reusa el normalizador de MisionFactory y no un valueOf con trim/toUpperCase
            // propio (punto 34). Con el valueOf pelado, "CATEGORÍA" con acento —que es como
            // lo escribe una persona— daba 400 con el mensaje crudo de
            // IllegalArgumentException, mientras que el POST de la misma misión sí
            // aceptaba esa cadena. Era la misma entrada por dos caminos y solo uno
            // entendía español.
            try {
                atributo = AtributoImpacto.valueOf(MisionFactory.normalizar(atributoStr));
            } catch (IllegalArgumentException sinNormalizar) {
                // Antes el IllegalArgumentException desnudo sobrevivía porque
                // GlobalExceptionHandler lo mapea a 400. Eso ataba el mensaje a un handler que
                // puede cambiar, y el mensaje era "No enum constant ...", que no le dice
                // nada a quien está usando la API.
                throw new DatosInvalidosException(
                        "'" + atributoStr + "' no es un atributo de impacto válido. Se aceptan: "
                                + List.of(AtributoImpacto.values()));
            }
        }

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
     *
     * <p><b>El resultado sale en el orden en que los pidió el admin</b> (punto 27).
     * {@code findAllById} genera un {@code SELECT ... WHERE id IN (...)} sin
     * {@code ORDER BY}, así que el orden con que vuelve es el que devuelva la base. Ese
     * orden importa: {@code Categoria.agregarMision} va asignando
     * {@code posicion = size + 1}, o sea que el orden de la lista <b>es</b> la secuencia de
     * progresión del donante. Sin reordenar, el donante puede arrancar en otra misión.
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
