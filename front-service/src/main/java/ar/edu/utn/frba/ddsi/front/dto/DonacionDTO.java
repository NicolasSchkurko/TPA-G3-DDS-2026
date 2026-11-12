package ar.edu.utn.frba.ddsi.front.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DonacionDTO {
    private UUID id;
    private String donanteName;
    private String entidadBeneficiaria;
    private String descripcion;
    private String estado;
    private String subcategoriaName;
    private String categoriaBienName;
    private LocalDate fechaEntrega;
    private Integer cantidadTotalBienes;
    private List<String> imagenes = new ArrayList<>();

    /**
     * Bienes de la donación. GET /api/donaciones los devuelve embebidos, así que el panel de
     * detalle y el formulario de edición se arman sin un segundo request.
     */
    private List<BienDonacionDTO> bienes = new ArrayList<>();


    public List<String> imagenesVisibles() {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        if (imagenes != null) {
            imagenes.stream().filter(this::urlValida).forEach(urls::add);
        }
        if (bienes != null) {
            bienes.stream().map(BienDonacionDTO::getUrlFoto).filter(this::urlValida).forEach(urls::add);
        }
        return new ArrayList<>(urls);
    }

    public String imagenPrincipal() {
        return imagenesVisibles().stream().findFirst().orElse(null);
    }

    private boolean urlValida(String url) {
        return url != null && !url.isBlank();
    }

    public String tituloVisible() {
        if (subcategoriaName != null && !subcategoriaName.isBlank()) {
            return subcategoriaName;
        }
        return descripcion != null && !descripcion.isBlank() ? descripcion : "Donación";
    }

    public String descripcionVisible() {
        if (descripcion == null || descripcion.isBlank()) {
            return "";
        }
        String limpia = descripcion.replaceFirst("(?i)^Segmento de donación:\\s*", "").trim();
        return limpia.equalsIgnoreCase(subcategoriaName) ? "" : limpia;
    }

    /** El backend escribe "No asignada" cuando la donación todavía no tiene entidad. */
    public boolean tieneEntidad() {
        return entidadBeneficiaria != null
                && !entidadBeneficiaria.isBlank()
                && !"No asignada".equalsIgnoreCase(entidadBeneficiaria);
    }

    /**
     * Los bienes sólo se pueden reescribir mientras la donación está en depósito: después, los
     * conteos de las necesidades ya cuentan lo entregado y el PUT del backend responde 400
     * (ver DonacionService.actualizarDonacion).
     */
    public boolean puedeEditarBienes() {
        return "EN_DEPOSITO".equalsIgnoreCase(estado);
    }

    public String etiquetaEstado() {
        return etiquetaDeEstado(estado);
    }

    /** Sufijo de clase CSS para el badge de estado (estado-en-deposito, estado-entregado...). */
    public String claseEstado() {
        return claseDeEstado(estado);
    }

    /** Nombre legible de cada Estado del backend. */
    public static String etiquetaDeEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return "Sin estado";
        }
        return switch (estado.toUpperCase()) {
            case "EN_DEPOSITO" -> "En depósito";
            case "PENDIENTE_ASIGNACION" -> "Pendiente de asignación";
            case "ASIGNADO" -> "Asignada";
            case "LISTA_PARA_ENTREGAR" -> "Lista para entregar";
            case "EN_TRASLADO" -> "En camino";
            case "ENTREGADO" -> "Entregada";
            case "ENTREGA_FALLIDA" -> "Entrega fallida";
            case "VENCIDO" -> "Vencida";
            default -> estado;
        };
    }

    public static String claseDeEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return "estado-desconocido";
        }
        return "estado-" + estado.toLowerCase().replace('_', '-');
    }
}
