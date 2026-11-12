package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para Administrador.
 * No se usa directamente desde los Gestores/Services: RepositorioDeAdministradores actúa como
 * fachada, manteniendo los métodos públicos que ya existían cuando era un repositorio en memoria.
 */
public interface RepositorioDeAdministradores extends JpaRepository<Administrador, UUID> {

    /** Administrador vinculado a la cuenta de Keycloak indicada. */
    Optional<Administrador> findByKeycloakSub(String keycloakSub);

    @Query("""
        SELECT DISTINCT a FROM Administrador a
        LEFT JOIN a.humano h
        LEFT JOIN h.mediosDeContacto hc
        LEFT JOIN hc.listaMediosDeContacto hm
        WHERE (TYPE(a.medioDeContacto) = Mail
               AND LOWER(TREAT(a.medioDeContacto AS Mail).direccionMail) = LOWER(:email))
           OR (TYPE(hm) = Mail
               AND LOWER(TREAT(hm AS Mail).direccionMail) = LOWER(:email))
        """)
    List<Administrador> buscarPorEmailContactoList(@Param("email") String email);

    default void guardar(Administrador administrador) {
        if (administrador != null) {
            if (existsById(administrador.getId())) {
                throw new IllegalArgumentException("Ya existe un administrador con el ID: " + administrador.getId());
            }
            save(administrador);
        }
    }

    default List<Administrador> obtenerTodos() { return findAll(); }

    default Optional<Administrador> buscarPorId(UUID id) { return findById(id); }

    /** Administrador vinculado a la cuenta de Keycloak indicada. */
    default Optional<Administrador> buscarPorKeycloakSub(String keycloakSub) {
        if (keycloakSub == null || keycloakSub.isBlank()) {
            return Optional.empty();
        }
        return findByKeycloakSub(keycloakSub.trim());
    }

    /** Administrador vinculado al mail indicado (el primero si hay duplicados). */
    default Optional<Administrador> buscarPorEmailContacto(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        List<Administrador> resultados = buscarPorEmailContactoList(email.trim().toLowerCase());
        return resultados.isEmpty() ? Optional.empty() : Optional.of(resultados.get(0));
    }

    default void actualizar(UUID idOriginal, Administrador adminActualizado) {
        if (existsById(idOriginal)) {
            adminActualizado.setId(idOriginal);
            save(adminActualizado);
        } else {
            throw new IllegalArgumentException("No se encontró el administrador a actualizar.");
        }
    }

    default void eliminarPorId(UUID id) { deleteById(id); }

}
