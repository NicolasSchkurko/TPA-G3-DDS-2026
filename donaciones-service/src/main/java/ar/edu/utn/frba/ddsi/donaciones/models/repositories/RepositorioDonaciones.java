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

    public List<Donacion> obtenerTodos() {
        return new ArrayList<>(donaciones);
    }

    public Optional<Donacion> obtenerPorId(UUID id) {
        return this.donaciones.stream()
                         .filter(d -> d.getId().equals(id))
                         .findFirst();
    }

    public List<Donacion> buscarDonacionesEnDeposito() {
        return this.donaciones.stream()
                         .filter(d -> d.getEstado() == Estado.EN_DEPOSITO)
                         .toList();
    }

    public List<Donacion> buscarDonacionesPendientesDeAsignar(){
        return this.donaciones.stream()
                .filter(d -> d.getEstado() == Estado.PENDIENTE_ASIGNACION)
                .toList();
    }

    public List<Donacion> buscarDonacionesSinAsignar(){
        return this.donaciones.stream()
                .filter(d -> d.getEstado() == Estado.EN_DEPOSITO)
                .toList();
    }

    public List<Donacion> buscarEntregaPendiente() {
        return this.donaciones.stream()
                         .filter(d -> d.getEstado() == Estado.ASIGNADO)
                         .toList();
    }

    public void guardarDonaciones(List<Donacion> donacionesFormulario) {
        this.donaciones.addAll(donacionesFormulario);
    }

    public void guardar(Donacion donacion) {
        this.donaciones.add(donacion);
    }

    public Optional<Donacion> actualizar(UUID idOriginal, Donacion donacionActualizada) {
        Optional<Donacion> donacionExistente = obtenerPorId(idOriginal);
        if (donacionExistente.isPresent()) {
            int index = this.donaciones.indexOf(donacionExistente.get());
            this.donaciones.set(index, donacionActualizada);

            return donacionExistente;
        } else {
            throw new IllegalArgumentException("No se encontró la donación a actualizar.");
        }
    }

    public void eliminarPorId(UUID id) {
        donaciones.removeIf(d -> d.getId().equals(id));
    }
}