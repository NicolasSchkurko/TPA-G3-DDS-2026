package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para Donante.
 * No se usa directamente desde los Gestores/Services: RepositorioDeDonantes actúa como
 * fachada, manteniendo los métodos públicos que ya existían cuando era un repositorio en memoria.
 */
public interface RepositorioDeDonantes extends JpaRepository<Donante, UUID> {

  @Query("SELECT d FROM Donante d WHERE d.activo = true AND ("
      + " (TYPE(d.persona) = Humana AND ("
      + "   LOWER(TREAT(d.persona AS Humana).nombre) = LOWER(:nombre) OR "
      + "   LOWER(TREAT(d.persona AS Humana).apellido) = LOWER(:nombre) OR "
      + "   LOWER(CONCAT(TREAT(d.persona AS Humana).nombre, ' ', TREAT(d.persona AS Humana).apellido)) = LOWER(:nombre)"
      + " )) OR "
      + " (TYPE(d.persona) = Juridica AND LOWER(TREAT(d.persona AS Juridica).razonSocial) = LOWER(:nombre))"
      + ")")
  List<Donante> buscarPorNombreActivo(@Param("nombre") String nombre);

  @Query("""
      SELECT DISTINCT d FROM Donante d
      JOIN d.persona p
      JOIN p.mediosDeContacto mc
      JOIN mc.listaMediosDeContacto m
      WHERE TYPE(m) = Mail
        AND LOWER(TREAT(m AS Mail).direccionMail) = LOWER(:email)
        AND d.activo = true
      """)
  List<Donante> buscarPorEmailContacto(@Param("email") String email);

  Page<Donante> findByActivoTrue(Pageable pageable);

  /**
   * Donantes activos cuya última donación es anterior a {@code limite} y que no recibieron
   * el aviso de inactividad desde esa última donación (punto 67).
   */
  @Query("""
      SELECT d FROM Donante d
      WHERE d.activo = true
        AND (SELECT MAX(f.fechaRealizacion) FROM Formulario f WHERE f.donante = d) < :limite
        AND (d.ultimaNotificacionInactividad IS NULL
             OR d.ultimaNotificacionInactividad < (SELECT MAX(f2.fechaRealizacion) FROM Formulario f2 WHERE f2.donante = d))
      ORDER BY d.id
      """)
  List<Donante> buscarInactivosParaNotificar(@Param("limite") LocalDate limite,
                                             Pageable pageable);

    default void guardar(Donante donante) {
        if (donante != null && donante.getId() != null) {
            if (existsById(donante.getId())) {
                throw new IllegalArgumentException("Ya existe un donante con el ID: " + donante.getId());
            }
            save(donante);
        }
    }

    default List<Donante> obtenerTodos() { return findAll(); }

    default Page<Donante> obtenerTodosActivos(Pageable pageable) { return findByActivoTrue(pageable); }

    default Optional<Donante> buscarPorId(UUID id) { return findById(id); }

    default Optional<Donante> buscarPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return Optional.empty();
        }
        List<Donante> resultados = buscarPorNombreActivo(nombre.trim());
        return resultados.isEmpty() ? Optional.empty() : Optional.of(resultados.get(0));
    }

    default void actualizar(UUID idOriginal, Donante donanteActualizado) {
        if (existsById(idOriginal)) {
            save(donanteActualizado);
        } else {
            throw new IllegalArgumentException("No se encontró el donante a actualizar.");
        }
    }

    default void eliminarPorId(UUID id) { deleteById(id); }

    default Donante modificarDonante(UUID idOriginal, Donante datosNuevos) {
        Donante existente = findById(idOriginal)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró el donante con ID: " + idOriginal));
        existente.setDireccion(datosNuevos.getDireccion());
        save(existente);
        return existente;
    }

    default void agregarFormularioADonante(UUID idDonante, Formulario nuevoFormulario) {
        findById(idDonante).ifPresent(donante -> {
            donante.agregarFormulario(nuevoFormulario);
            save(donante);
        });
    }

}
