package ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Una posición del ranking de un mes. No se edita. El nombre del donante está desnormalizado
 * para que el podio se lea sin cargar el perfil.
 */
@Getter
@Entity
@NoArgsConstructor
public class Ranking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ranking_mensual_id")
    private RankingMensual rankingMensual;

    private UUID idUsuario; // Para saber quién es sin cargar todo el perfil
    private String nombreUsuario; // Desnormalizado para lectura rápida en frontend

    private Integer puesto;
    private Long misionesCumplidas; // Lo que calculó el SQL

    public Ranking(RankingMensual rankingMensual,
                   UUID idUsuario,
                   String nombreUsuario,
                   Integer puesto,
                   Long misionesCumplidas) {
        this.rankingMensual = rankingMensual;
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.puesto = puesto;
        this.misionesCumplidas = misionesCumplidas;
    }
}
