package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.BienResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.BienConEstado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.BienPerecedero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.UnidadDeMedida;
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

    public Donacion actualizar(UUID id, DonacionDTO donacion) {
        if (donacion == null) {
            return null;
        }

        Optional<Donacion> existente = this.findById(id);
        if (existente.isPresent()) {
            Donacion donacionActualizada = existente.get();

            // Actualizar campos simples
            if (donacion.getDescripcion() != null) {
                donacionActualizada.setDescripcion(donacion.getDescripcion());
            }
            if (donacion.getFechaEntrega() != null) {
                donacionActualizada.setFechaEntrega(donacion.getFechaEntrega());
            }
            if (donacion.getEstado() != null) {
                donacionActualizada.setEstado(Estado.valueOf(donacion.getEstado().toUpperCase()));
            }

            // Subcategoria y categoria (si vienen en el DTO)
            if (donacion.getSubcategoriaName() != null || donacion.getCategoriaBienName() != null) {
                String nombreCat = donacion.getCategoriaBienName() != null ? donacion.getCategoriaBienName() : "";
                String nombreSub = donacion.getSubcategoriaName() != null ? donacion.getSubcategoriaName() : "";
                CategoriaBien categoria = new CategoriaBien(nombreCat);
                SubcategoriaBien sub = new SubcategoriaBien(nombreSub, categoria);
                donacionActualizada.setSubcategoria(sub);
            }

            // Bienes (reemplaza la lista si viene en el DTO)
            if (donacion.getBienes() != null) {
                List<Bien> nuevosBienes = new ArrayList<>();
                for (BienResumenDTO b : donacion.getBienes()) {
                    String nombreCat = b.getCategoria();
                    String nombreSub = b.getSubcategoria();
                    CategoriaBien categoria = new CategoriaBien(nombreCat != null ? nombreCat : "");
                    SubcategoriaBien sub = new SubcategoriaBien(nombreSub != null ? nombreSub : "", categoria);

                    UnidadDeMedida unidad = UnidadDeMedida.UNIDADES;
                    if (b.getUnidadDeMedida() != null) {
                        try {
                            unidad = UnidadDeMedida.valueOf(b.getUnidadDeMedida().toUpperCase());
                        } catch (IllegalArgumentException ignored) {
                            // dejar UNIDADES por defecto
                        }
                    }

                    Integer cantidad = b.getCantidad() != null ? b.getCantidad() : 0;

                    if ("PERECEDERO".equalsIgnoreCase(b.getTipoBien())) {
                        nuevosBienes.add(new BienPerecedero(b.getDescripcion(), sub, cantidad, unidad, b.getFechaVencimiento()));
                    } else {
                        boolean usado = b.getUsado() != null ? b.getUsado() : false;
                        nuevosBienes.add(new BienConEstado(b.getDescripcion(), sub, cantidad, unidad, usado));
                    }
                }
                donacionActualizada.setBienes(nuevosBienes);
            }

            // Reemplazar la instancia en el repositorio (lista)
            int index = donaciones.indexOf(donacionActualizada);
            if (index >= 0) {
                donaciones.set(index, donacionActualizada);
            }

            return donacionActualizada;
        }

        return null;
    }

    public void deleteById(UUID id) {
        donaciones.removeIf(d -> d.getId().equals(id));
    }
}