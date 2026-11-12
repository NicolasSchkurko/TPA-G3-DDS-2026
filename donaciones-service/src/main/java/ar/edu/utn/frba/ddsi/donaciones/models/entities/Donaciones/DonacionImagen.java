package ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "donacion_imagen")
@Getter
@Setter
@NoArgsConstructor
public class DonacionImagen {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne
    @JoinColumn(name = "donacion_id", nullable = false)
    private Donacion donacion;

    @Column(name = "url_foto", nullable = false, length = 500)
    private String urlFoto;

    @Column(name = "orden", nullable = false)
    private int orden;

    public DonacionImagen(Donacion donacion, String urlFoto, int orden) {
        this.donacion = donacion;
        this.urlFoto = urlFoto;
        this.orden = orden;
    }
}
