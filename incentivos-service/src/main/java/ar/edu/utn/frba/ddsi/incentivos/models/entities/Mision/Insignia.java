package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Insignia {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idInsignia;
    private String nombre;
    private String descripcion;
    private String urlImagen;


    public Insignia(String nombreInsignia, String descripcion){
        this.nombre = nombreInsignia;

        this.urlImagen = null;
        this.descripcion = descripcion;
    }
}