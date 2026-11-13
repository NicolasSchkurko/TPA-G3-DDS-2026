package ar.edu.utn.frba.ddsi.front.dto;

import ar.edu.utn.frba.ddsi.front.dto.logistica.LogisticaDTOs;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Datos que consume la pantalla del chofer. Se arma en el front a partir de
 * {@code logisticas-service}: perfil de chofer, camión, ruta activa, paradas e historial.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChoferDashboardDTO {

    private boolean perfilEncontrado;
    private ChoferDTO chofer;
    private String usuario;
    private LogisticaDTOs.Camion camion;
    private LogisticaDTOs.Ruta rutaActiva;
    private DepositoVista deposito;
    private List<ParadaVista> paradas = new ArrayList<>();
    private List<LogisticaDTOs.Ruta> historial = new ArrayList<>();
    private int entregasPendientes;
    private int entregasTotales;
    private boolean hayTrazado;
    private String geojson = "{\"type\":\"FeatureCollection\",\"features\":[]}";
    private int historialPagina;
    private int historialTotalPaginas;
    private long historialTotalElementos;
    private boolean historialTieneAnterior;
    private boolean historialTieneSiguiente;

    public boolean isRutaEnCurso() {
        return rutaActiva != null && "EN_CURSO".equalsIgnoreCase(rutaActiva.getEstado());
    }

    public boolean isRutaProgramada() {
        return rutaActiva != null && "PROGRAMADA".equalsIgnoreCase(rutaActiva.getEstado());
    }

    public boolean isTodasEntregadas() {
        return entregasTotales > 0 && entregasPendientes == 0;
    }

    public String getPatente() {
        if (camion != null && camion.getPatente() != null) return camion.getPatente();
        if (rutaActiva != null) return rutaActiva.patente();
        return "Sin asignar";
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DepositoVista {
        private String etiqueta;
        private LogisticaDTOs.DireccionLogistica direccion;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ParadaVista {
        private int numero;
        private LogisticaDTOs.DireccionLogistica direccion;
        private List<BienVista> bienes = new ArrayList<>();
        private boolean actual;
        private boolean completa;
        private int entregadas;
        private int total;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class BienVista {
        private UUID idDonacion;
        private Integer cantidad;
        private String unidad;
        private String estado;
        private boolean entregada;
        private String estadoEtiqueta;

        public String getCantidadConUnidad() {
            String cantidadTexto = cantidad != null ? String.valueOf(cantidad) : "—";
            return unidad != null && !unidad.isBlank() ? cantidadTexto + " " + unidad : cantidadTexto;
        }

        public String getIdCorto() {
            return idDonacion != null ? idDonacion.toString().substring(0, 8) : "";
        }
    }
}
