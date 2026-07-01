package ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas;

import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Entrega {
    private List<UUID> idsDonaciones;
    private List<DonacionResumen> donacionResumen; //peso de cada bien
    private DireccionEntidad entidadBeneficiaria;
}
