package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioRutas extends MongoRepository<Ruta, UUID> {

    Optional<Ruta> findFirstByParadasItemsIdDonacion(UUID idDonacion);

    Optional<Ruta> findFirstByCamionAsignadoChoferIdChoferAndEstado(UUID idChofer, EstadoRuta estado);

    List<Ruta> findByEstadoIn(Collection<EstadoRuta> estados);

    Page<Ruta> findByCamionAsignadoChoferIdChofer(UUID idChofer, Pageable pageable);
}
