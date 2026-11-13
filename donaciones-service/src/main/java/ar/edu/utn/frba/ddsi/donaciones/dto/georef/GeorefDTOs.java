package ar.edu.utn.frba.ddsi.donaciones.dto.georef;

import java.util.List;

/**
 * Contrato mínimo con la API Georef de datos.gob.ar.
 *
 * <p>Solo se modelan los campos que usa el servicio. El endpoint de direcciones acepta
 * consultas individuales por GET y lotes por POST; acá se usa siempre el POST, incluso para
 * una sola dirección, para tener un único camino de parseo.
 */
public final class GeorefDTOs {

    private GeorefDTOs() {
    }

    /** Cuerpo del POST /direcciones. */
    public record Lote(List<Consulta> direcciones) {
    }

    /**
     * Una consulta. {@code max = 1} pide solo el mejor resultado por dirección, que es lo que
     * recomienda la documentación para lotes y además mantiene el orden de la respuesta.
     */
    public record Consulta(String direccion, String provincia, String localidad, int max) {
        public static Consulta de(String direccion, String provincia, String localidad) {
            return new Consulta(direccion, provincia, localidad, 1);
        }
    }

    public record Respuesta(List<Resultado> resultados) {
    }

    public record Resultado(List<Direccion> direcciones) {
    }

    public record Direccion(Ubicacion ubicacion) {
    }

    /** WGS84. {@code lat}/{@code lon} vienen nulos si Georef no pudo aproximar la ubicación. */
    public record Ubicacion(Double lat, Double lon) {
    }
}
