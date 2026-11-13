package ar.edu.utn.frba.ddsi.logisticas.dto.rutas;
import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RutaDTO {
    private UUID idRuta;
    private CamionDTO camionAsignado;
    private LocalDate fechaProgramada;
    private String estado;
    private String urlSeguimiento;
    private List<ParadaDTO> paradas;
    private DepositoDTO deposito;

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

    /** Copia los datos de la ruta sobre este DTO. La ruta no puede ser {@code null}. */
    public RutaDTO aplicar(Ruta ruta) {
        this.idRuta = ruta.getIdRuta();
        this.camionAsignado = ruta.getCamionAsignado() == null
                ? null
                : new CamionDTO().aplicar(ruta.getCamionAsignado());
        this.fechaProgramada = ruta.getFechaProgramada();
        this.estado = ruta.getEstado() != null ? ruta.getEstado().toString() : null;
        this.urlSeguimiento = ruta.getUrlSeguimiento();
        this.paradas = ruta.getParadas() == null
                ? List.of()
                : ruta.getParadas().stream()
                        .map(parada -> new ParadaDTO().aplicar(parada))
                        .toList();
        return this;
    }
}
