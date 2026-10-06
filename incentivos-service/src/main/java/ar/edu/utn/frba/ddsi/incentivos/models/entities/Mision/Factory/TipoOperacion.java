package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory;

/**
 * Qué cuenta como progreso, o sea qué implementación de {@code Operacion} se construye.
 *
 * <p>El nombre del valor es el que viaja en el JSON del admin y el que elige
 * {@code OperacionFactory} mediante un {@code switch}.
 */
public enum TipoOperacion {
    COINCIDENCIAS,
    VALORES_DISTINTOS,
    SUPERA_CANTIDAD
}
