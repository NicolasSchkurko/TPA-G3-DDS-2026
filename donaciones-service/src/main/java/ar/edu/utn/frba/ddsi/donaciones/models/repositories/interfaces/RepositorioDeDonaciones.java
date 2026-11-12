package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Repositorio Spring Data JPA para Donacion.
 * No se usa directamente desde los Gestores/Services: RepositorioDeDonaciones actúa como
 * fachada, manteniendo los métodos públicos que ya existían cuando era un repositorio en memoria.
 */
public interface RepositorioDeDonaciones extends JpaRepository<Donacion, UUID> {
    List<Donacion> findByEstado(Estado estado);

    List<Donacion> findByDonante_Id(UUID donanteId);

    @Query("SELECT d.id FROM Donacion d WHERE d.estado = :estado")
    List<UUID> findIdsByEstado(@Param("estado") Estado estado);

    @Query("""
        SELECT d.entidad.id AS entidadId, COUNT(d) AS cantidad
        FROM Donacion d
        WHERE d.estado = :estado
          AND d.fechaEntrega IS NOT NULL
          AND d.fechaEntrega > :desde
          AND d.entidad IS NOT NULL
        GROUP BY d.entidad.id
        """)
    List<ConteoEntregasPorEntidad> contarEntregadasPorEntidadDesde(
        @Param("estado") Estado estado, @Param("desde") LocalDate desde);

    @Query("""
        SELECT d.necesidad.id AS necesidadId, d.fechaEntrega AS fechaEntrega,
               COALESCE(SUM(b.peso), 0) AS cantidad
        FROM Donacion d JOIN d.bienes b
        WHERE d.estado = :estado
          AND d.necesidad IS NOT NULL
        GROUP BY d.necesidad.id, d.fechaEntrega
        """)
    List<EntregaPorNecesidad> sumarEntregadasPorNecesidadYFecha(
        @Param("estado") Estado estado);

    interface EntregaPorNecesidad {
        UUID getNecesidadId();

        LocalDate getFechaEntrega();

        Long getCantidad();
    }

    interface ConteoEntregasPorEntidad {
        UUID getEntidadId();

        Long getCantidad();
    }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Donacion d SET d.donante = null WHERE d.donante.id = :donanteId")
    void desvincularDonante(@Param("donanteId") UUID donanteId);

    default List<Donacion> obtenerTodos() { return findAll(); }

    default Page<Donacion> obtenerTodos(Pageable pageable) { return findAll(pageable); }

    default Optional<Donacion> obtenerPorId(UUID id) { return findById(id); }

    default List<Donacion> buscarDonacionesPendientesDeAsignar() {
        return findByEstado(Estado.PENDIENTE_ASIGNACION);
    }

    default List<Donacion> buscarDonacionesSinAsignar() { return findByEstado(Estado.EN_DEPOSITO); }

    default List<UUID> buscarIdsSinAsignar() { return findIdsByEstado(Estado.EN_DEPOSITO); }

    default Map<UUID, List<EntregaNecesidad>> entregasPorNecesidadYFecha() {
        return sumarEntregadasPorNecesidadYFecha(Estado.ENTREGADO).stream()
            .collect(Collectors.groupingBy(
                EntregaPorNecesidad::getNecesidadId,
                Collectors.mapping(
                    entrega -> new EntregaNecesidad(
                        entrega.getFechaEntrega(), entrega.getCantidad().intValue()),
                    Collectors.toList())));
    }

    record EntregaNecesidad(LocalDate fechaEntrega, int cantidad) {
    }

    default Map<UUID, Integer> contarEntregadasUltimoTrimestrePorEntidad(LocalDate desde) {
        return contarEntregadasPorEntidadDesde(Estado.ENTREGADO, desde).stream()
            .collect(Collectors.toMap(
                ConteoEntregasPorEntidad::getEntidadId,
                conteo -> conteo.getCantidad().intValue()));
    }

    default List<Donacion> buscarEntregaPendiente() { return findByEstado(Estado.LISTA_PARA_ENTREGAR); }

    default void guardarDonaciones(List<Donacion> donaciones) { saveAll(donaciones); }

    default void guardar(Donacion donacion) { save(donacion); }

    /** Donaciones del donante indicado (para su historial). */
    default List<Donacion> buscarPorDonante(UUID donanteId) {
        if (donanteId == null) {
            return List.of();
        }
        return findByDonante_Id(donanteId);
    }

    default Optional<Donacion> actualizar(UUID idOriginal, Donacion donacionActualizada) {
        if (existsById(idOriginal)) {
            return Optional.of(save(donacionActualizada));
        }
        throw new IllegalArgumentException("No se encontró la donación a actualizar.");
    }

    default void asignarEntidad(UUID donacionId, EntidadBeneficiaria entidad) {
        Donacion donacion = findById(donacionId)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró la donación con ID: " + donacionId));
        donacion.setEntidad(entidad);
        save(donacion);
    }

    default void eliminarPorId(UUID id) { deleteById(id); }

}
