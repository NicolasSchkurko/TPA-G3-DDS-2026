package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fachada sobre DonacionJpaRepository (Spring Data JPA).
 * Mantiene la misma interfaz pública que tenía cuando era un repositorio en memoria.
 */
@Repository
public class RepositorioDonaciones {

    private final DonacionJpaRepository jpaRepository;

    public RepositorioDonaciones(DonacionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public List<Donacion> obtenerTodos() {
        return jpaRepository.findAll();
    }

    public Optional<Donacion> obtenerPorId(UUID id) {
        return jpaRepository.findById(id);
    }

    public List<Donacion> buscarDonacionesEnDeposito() {
        return jpaRepository.findByEstado(Estado.EN_DEPOSITO);
    }

    public List<Donacion> buscarDonacionesPendientesDeAsignar(){
        return jpaRepository.findByEstado(Estado.PENDIENTE_ASIGNACION);
    }

    public List<Donacion> buscarDonacionesSinAsignar(){
        return jpaRepository.findByEstado(Estado.EN_DEPOSITO);
    }

    public List<Donacion> buscarEntregaPendiente() {
        return jpaRepository.findByEstado(Estado.ASIGNADO);
    }

    public void guardarDonaciones(List<Donacion> donacionesFormulario) {
        jpaRepository.saveAll(donacionesFormulario);
    }

    public void guardar(Donacion donacion) {
        jpaRepository.save(donacion);
    }

    // A diferencia de la versión en memoria (que devolvía la donación VIEJA por error), acá
    // devolvemos la donación ya guardada/actualizada: con persistencia real, devolver el estado
    // previo induciría a pensar que el PUT no aplicó los cambios (que sí se guardaron en la DB).
    public Optional<Donacion> actualizar(UUID idOriginal, Donacion donacionActualizada) {
        if (jpaRepository.existsById(idOriginal)) {
            return Optional.of(jpaRepository.save(donacionActualizada));
        } else {
            throw new IllegalArgumentException("No se encontró la donación a actualizar.");
        }
    }

    // Con JPA, a diferencia del repositorio en memoria (donde mutar el objeto ya alcanzaba
    // porque era la misma instancia guardada en la lista), hay que guardar explícitamente el
    // cambio: lo que devuelve buscarPorId/findById queda detached apenas termina la transacción.
    public void asignarEntidad(UUID donacionId, EntidadBeneficiaria entidad) {
        Donacion donacion = jpaRepository.findById(donacionId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la donación con ID: " + donacionId));

        donacion.setEntidad(entidad);
        jpaRepository.save(donacion);
    }

    public void eliminarPorId(UUID id) {
        jpaRepository.deleteById(id);
    }
}