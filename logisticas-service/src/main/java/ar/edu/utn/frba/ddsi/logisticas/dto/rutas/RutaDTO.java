package ar.edu.utn.frba.ddsi.logisticas.dto.rutas;
import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class RutaDTO {
    private UUID idRuta;
    private CamionDTO camionAsignado;
    private LocalDate fechaProgramada;
    private String estado;
    private String urlSeguimiento;
    private List<ParadaDTO> paradas;

    public RutaDTO(UUID idRuta,
                   CamionDTO camionAsignado,
                   LocalDate fechaProgramada,
                   String estado,
                   String urlSeguimiento,
                   List<ParadaDTO> paradas){
        this.idRuta = idRuta;
        this.camionAsignado = camionAsignado;
        this.fechaProgramada = fechaProgramada;
        this.estado = estado;
        this.urlSeguimiento = urlSeguimiento;
        this.paradas = paradas;
    }
}
