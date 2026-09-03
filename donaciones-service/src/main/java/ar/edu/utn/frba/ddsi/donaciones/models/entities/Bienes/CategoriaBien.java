package ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoriaBien {

    @Id
    private UUID id = UUID.randomUUID();

    private String nombre;

    @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL)
    private List<SubcategoriaBien> subcategorias = new ArrayList<>();

    public CategoriaBien(String nombre) {
        this.nombre = nombre;
        this.subcategorias = new ArrayList<>();
    }

    public void agregarSubcategoria(SubcategoriaBien subcategoria) {
        this.subcategorias.add(subcategoria);
    }

    @Override
    public String toString() {
        return "CategoriaBien{nombre=" + nombre + '}';
    }
}
