package ar.edu.utn.frba.ddsi.front.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Vista consolidada del seguimiento de entregas activas de una entidad. */
@Getter
@Setter
@NoArgsConstructor
public class SeguimientoDTO {
    private boolean ubicacionSimulada = true;
    private String generadoEn;
    private List<EntregaActiva> entregas = new ArrayList<>();
    private List<CamionEnRuta> camiones = new ArrayList<>();
    private Double depositoLat;
    private Double depositoLng;
    private String depositoEtiqueta;

    @Getter @Setter @NoArgsConstructor
    public static class EntregaActiva {
        private UUID idDonacion;
        private String estado;
        private Integer cantidad;
        private String unidadDeMedida;
        private String destino;
        private String zona;
        private Double lat;
        private Double lng;
    }

    @Getter @Setter @NoArgsConstructor
    public static class CamionEnRuta {
        private String patente;
        private String chofer;
        private String estadoRuta;
        private Double lat;
        private Double lng;
        private String ultimaActualizacion; // ISO-8601
        private Integer etaMinutos;
        private String urlSeguimiento;
        /** Token opaco para abrir el mapa completo sin exponer el UUID de la ruta. */
        private String tokenSeguimiento;
        private Integer paradaActual;
        private String destino;
        private List<List<Double>> trazado = new ArrayList<>();
        private List<UUID> donaciones = new ArrayList<>();
    }
}
