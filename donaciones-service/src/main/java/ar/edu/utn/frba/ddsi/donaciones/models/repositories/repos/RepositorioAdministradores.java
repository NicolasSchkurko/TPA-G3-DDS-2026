package ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.AdministradorJpaRepository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Fachada sobre AdministradorJpaRepository (Spring Data JPA).
 * Mantiene la misma interfaz pública que tenía cuando era un repositorio en memoria
 * (incluyendo que buscarPorId lanza si no encuentra, tal cual el comportamiento original).
 */
@Repository
public class RepositorioAdministradores {

    private final AdministradorJpaRepository jpaRepository;

    public RepositorioAdministradores(AdministradorJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public void guardar(Administrador administrador) {
        if (administrador != null) {
            if (jpaRepository.existsById(administrador.getId())) {
                throw new IllegalArgumentException("Ya existe un administrador con el ID: " + administrador.getId());
            }
            jpaRepository.save(administrador);
        }
    }

    public List<Administrador> obtenerTodos() {
        return jpaRepository.findAll();
    }

/**
     * Busca por id. <b>Devuelve {@code Optional.empty()} si no existe, no lanza.</b>
     *
     * <p><b>Por qué este método cambió y antes no.</b> La clase está anotada {@code @Repository},
     * así que Spring le envuelve un {@code PersistenceExceptionTranslationInterceptor}. Cuando el
     * método lanzaba {@code IllegalArgumentException}, ese interceptor lo envolvía en
     * {@code InvalidDataAccessApiUsageException}:
     *
     * <pre>
     * org.springframework.dao.InvalidDataAccessApiUsageException: No se encontró el administrador
     *   at RepositorioAdministradores$$SpringCGLIB$$0.buscarPorId
     * Caused by: java.lang.IllegalArgumentException
     * </pre>
     *
     * <p>Y como {@code InvalidDataAccessApiUsageException} <b>no</b> es
     * {@code IllegalArgumentException}, el {@code catch (IllegalArgumentException)} del
     * {@code AdminController} no lo agarraba: el pedido terminaba en el
     * {@code catch (Exception)} del {@code GlobalExceptionHandler} y devolvía <b>500</b> en vez del
     * 404 que el controlleritmás código abajo sabe armar.
     *
     * <p>Y eso no era un caso raro ni de test: {@code incentivos-service} consulta este endpoint
     * para saber si un usuario es administrador ({@code DonacionClient}), así que cualquier id que
     * no existiera le devolvía 500 al otro microservicio.
     *
     * <p><b>Un repositorio devuelve el dato, no decide si es un error.</b> Si no existe, eso es un
     * resultado valido de la consulta. Quien decide que "no encontré" es un error de negocio es el
     * service, con {@code orElseThrow}, y ahí la excepción no pasa por el traductor de Spring.
     *
     * <p>El resto de la capa de repositorios de este módulo tiene el mismo problema y est��
     * anotado en el {@code PENDIENTES.md} de {@code donaciones-service}.
     */
    public Optional<Administrador> buscarPorId(UUID id) {
        return jpaRepository.findById(id);
    }

    public void actualizar(UUID idOriginal, Administrador adminActualizado) {
        if (jpaRepository.existsById(idOriginal)) {
            jpaRepository.save(adminActualizado);
        } else {
            throw new IllegalArgumentException("No se encontró el administrador a actualizar.");
        }
    }

    public void eliminarPorId(UUID id) {
        jpaRepository.deleteById(id);
    }
}
