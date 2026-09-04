package ar.edu.utn.frba.ddsi.logisticas.models.repositories.camiones;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioCamiones extends JpaRepository<Camion, String> {

    Optional<Camion> findByChofer_IdChofer(UUID idChofer);

    default void resetearCarga(Camion camion){
        Optional<Camion> camionEncontrado = this.findById(camion.getPatente());
        if(camionEncontrado.isPresent()){
            camionEncontrado.get().setCiudadDestinoActual(null);
            camionEncontrado.get().resetearCargaOcupada();
            this.save(camionEncontrado.get());
        }
    }
}