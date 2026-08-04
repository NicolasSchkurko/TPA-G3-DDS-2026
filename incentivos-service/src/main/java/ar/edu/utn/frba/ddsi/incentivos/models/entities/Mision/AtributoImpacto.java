package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

public enum AtributoImpacto {
    ESTADO,
    CATEGORIA,
    CANTIDAD_BIENES,
    SUBCATEGORIA,
    ENTIDAD,
    FECHA;

    public Object obtenerDe(ImpactoDonacion impacto) {
        return switch (this) {
            case ESTADO -> impacto.getEstado();
            case CATEGORIA -> impacto.getCategoria();
            case CANTIDAD_BIENES -> impacto.getCantidadBienes();
            case SUBCATEGORIA -> impacto.getSubCategoria();
            case ENTIDAD -> impacto.getEntidadBeneficiaria();
            case FECHA -> impacto.getFechaEntrega();
        };
    }
}
