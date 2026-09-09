package ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ciudad")
@Getter
@Setter
@NoArgsConstructor
public class Ciudad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ciudad")
    private Long idCiudad;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "id_provincia", referencedColumnName = "id_provincia", nullable = false)
    private Provincia provincia;

    public Ciudad(String nombre, Provincia provincia){
        this.nombre = nombre;
        this.provincia = provincia;
    }
}