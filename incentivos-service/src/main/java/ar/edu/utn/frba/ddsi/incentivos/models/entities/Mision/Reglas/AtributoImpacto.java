package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas;

/**
 * Campo de la donación contra el que se mide el progreso de una misión.
 *
 * <p>El mismo campo pasa por dos traducciones distintas según dónde se use: contra
 * {@code ImpactoDonacion} cuando la misión acaba de completarse, y contra la fila que
 * guarda el donante de lo que vio la vez anterior cuando la operación es
 * {@code ValoresDistintos}, que necesita acordarse del valor.
 */
public enum AtributoImpacto {
    ESTADO,
    CATEGORIA,
    CANTIDAD_BIENES,
    SUBCATEGORIA,
    ENTIDAD,
    FECHA;
}
