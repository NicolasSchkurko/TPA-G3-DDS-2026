package ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubcategoriaBien {

    @Id
    private UUID id = UUID.randomUUID();

    private String nombre;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private CategoriaBien categoria;

    public SubcategoriaBien(String nombre, CategoriaBien categoria){
        this.nombre = nombre;
        this.categoria = categoria;
    }

    @Override
    public String toString() {
        return "SubcategoriaBien{nombre=" + nombre + ", categoria=" + categoria + '}';
    }
}
