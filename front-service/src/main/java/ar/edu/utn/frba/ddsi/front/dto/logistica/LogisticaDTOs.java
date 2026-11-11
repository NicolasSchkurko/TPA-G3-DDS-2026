package ar.edu.utn.frba.ddsi.front.dto.logistica;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** DTOs de lectura alineados con logisticas-service. Tolera campos faltantes. */
public final class LogisticaDTOs {

    private LogisticaDTOs() {
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DireccionLogistica {
        private UUID idEntidad;
        private String calleUno;
        private String calleDos;
        private Integer altura;
        private String ciudad;
        private String provincia;
        private String pais;

        public String etiqueta() {
            String calle = calleUno != null ? calleUno : "";
            String alturaStr = altura != null ? String.valueOf(altura) : "";
            String base = (calle + " " + alturaStr).trim();
            return base.isBlank() ? (ciudad != null ? ciudad : "Sin dirección") : base;
        }

        public String zona() {
            return ciudad != null && !ciudad.isBlank() ? ciudad : "CABA";
        }
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EventoLogistica {
        private Long id;
        private String tipoEvento;
        private String referenciaId;
        private String justificacion;
        private String payloadJson;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bien {
        private Integer cantidad;
        private String unidadDeMedida;
        private String estado;
        private LocalDateTime fechaCambioEstado;
        private String fotoComprobante;
        private DireccionLogistica entidadDestino;
        private List<EventoLogistica> eventos = List.of();

        public UUID idEntidadDestino() {
            return entidadDestino != null ? entidadDestino.getIdEntidad() : null;
        }

        public boolean estadoActivo() {
            if (estado == null) return false;
            return switch (estado.toUpperCase()) {
                case "PENDIENTE", "EN_CAMINO" -> true;
                default -> false;
            };
        }
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bienes {
        private List<UUID> idsDonaciones = List.of();
        private List<Bien> bienes = List.of();
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Camion {
        private UUID idChofer;
        private String patente;
        private Boolean disponible;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parada {
        private DireccionLogistica direccion;
        private Bienes items;

        public UUID idEntidadDestino() {
            return direccion != null ? direccion.getIdEntidad() : null;
        }
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ruta {
        private UUID idRuta;
        private Camion camionAsignado;
        private String fechaProgramada;
        private String estado;
        private String urlSeguimiento;
        private List<Parada> paradas = List.of();

        public boolean enCurso() {
            return estado != null && estado.equalsIgnoreCase("EN_CURSO");
        }

        public boolean contieneEntidad(UUID idEntidad) {
            if (idEntidad == null || paradas == null) return false;
            return paradas.stream().anyMatch(p -> idEntidad.equals(p.idEntidadDestino()));
        }

        public String patente() {
            return camionAsignado != null && camionAsignado.getPatente() != null ? camionAsignado.getPatente() : "Sin asignar";
        }
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rutas {
        private List<Ruta> rutas = List.of();
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActualizacionEntrega {
        private String estado;
        private String fotoUrl;
        private String justificacion;
    }
}
