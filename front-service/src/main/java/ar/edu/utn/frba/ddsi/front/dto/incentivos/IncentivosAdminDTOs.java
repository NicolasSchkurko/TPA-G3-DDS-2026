package ar.edu.utn.frba.ddsi.front.dto.incentivos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** DTOs de lectura alineados con incentivos-service. Tolera campos faltantes o nuevos. */
public final class IncentivosAdminDTOs {

    private IncentivosAdminDTOs() {
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Mision {
        private UUID idMision;
        private String nombreMision;
        private String descripcion;
        private String insigniaObjetivo;
        private String insigniaDescripcion;
        private String insigniaUrlImagen;
        private Regla regla;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Regla {
        private Constancia constancia;
        private String atributo;
        private Operacion operacion;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Constancia {
        private Integer cantidad;
        private String unidadTiempo;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Operacion {
        private String tipoOperacion;
        private Integer progresoObjetivo;
        private String valorEsperado;
        private Integer cantidad;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Categoria {
        private UUID idCategoria;
        private String nombre;
        private Integer posicionSecuencia;
        private List<UUID> misiones;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RankingMes {
        private UUID idRanking;
        /** El backend lo serializa como "2026-10". */
        private String periodo;
        private List<RankingItem> ranking;
        private Long participantes;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RankingItem {
        private String nombreUsuario;
        private Integer puesto;
        private Long misionesCumplidas;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RankingPage {
        private List<RankingItem> content = new ArrayList<>();
        private long totalElements;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RankingDetalle {
        private UUID idRanking;
        private String periodo;
        private RankingPage ranking;
    }
}
