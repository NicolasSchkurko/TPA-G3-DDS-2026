package ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class NecesidadRecurrente extends Necesidad {
    private Integer plazoEnDias;

    public NecesidadRecurrente(SubcategoriaBien subcategoria, String descripcion, Integer cantidadObjetivo, Integer plazoEnDias){
        super(subcategoria, descripcion, cantidadObjetivo);
        this.plazoEnDias = plazoEnDias;
    }

    @Override
    public boolean estaSatisfecha() {
        return this.cantidadRecibidaEnPeriodo() >= cantidadObjetivo;
    }

    public Integer cantidadRecibidaEnPeriodo() {
        LocalDate fechaLimite = LocalDate.now().minusDays(this.plazoEnDias);

        return this.getDonaciones().stream()
                .filter(donacion -> donacion.getEstado() == Estado.ENTREGADO)
                // Filtramos solo las donaciones cuya fecha de entrega sea posterior a la fecha límite
                .filter(donacion -> donacion.getFechaEntrega() != null && donacion.getFechaEntrega().isAfter(fechaLimite))
                .mapToInt(Donacion::sumaCantidadBienes)
                .sum();
    }

    @Override
    public String toString() {
        return "NecesidadRecurrente{subcategoria=" + subcategoria + ", descripcion=" + descripcion + ", cantidadObjetivo=" + cantidadObjetivo + ", plazoEnDias=" + plazoEnDias + '}';
    }
}
