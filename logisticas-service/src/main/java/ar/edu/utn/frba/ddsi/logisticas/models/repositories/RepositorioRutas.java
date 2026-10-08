package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioRutas extends JpaRepository<Ruta, UUID> {

    default Optional<Ruta> findByIdDonacion(UUID idDonacion){
        return this.findAll().stream()
                .filter(ruta -> ruta.obtenerTodosLosItems().stream()
                        .anyMatch(item -> item.getIdDonacion().equals(idDonacion)))
                .findFirst();
    }

    default Optional<Ruta> findByChofer(Chofer chofer){
        if (chofer == null) return Optional.empty();
        return this.findAll().stream()
                .filter(ruta -> ruta.getCamionAsignado() != null &&
                        chofer.equals(ruta.getCamionAsignado().getChofer()))
                .findFirst();
    }

    default Optional<Ruta> findByChoferYEstado(Chofer chofer, EstadoRuta estado) {
        if (chofer == null || estado == null) return Optional.empty();
        return this.findAll().stream()
                .filter(ruta -> ruta.getCamionAsignado() != null &&
                        chofer.equals(ruta.getCamionAsignado().getChofer()) &&
                        estado.equals(ruta.getEstado()))
                .findFirst();
    }
}