package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RepositorioDonaciones {
    // Simulamos una base de datos en memoria
    private final List<Donacion> donaciones = new ArrayList<>();

    public List<Donacion> findAll() {
        return new ArrayList<>(donaciones);
    }

    public Optional<Donacion> findById(UUID id) {
        return donaciones.stream()
                         .filter(d -> d.getId().equals(id))
                         .findFirst();
    }

    public List<Donacion> findPendient() {
        return donaciones.stream()
                         .filter(d -> d.getEstado() == Estado.EN_DEPOSITO)
                         .toList();
    }

    public void saveFormulario(List<Donacion> donacionesFormulario) {
        donaciones.addAll(donacionesFormulario);
    }

    public void save(Donacion donacion) {
        donaciones.add(donacion);
    }

    public void deleteById(UUID id) {
        donaciones.removeIf(d -> d.getId().equals(id));
    }
}