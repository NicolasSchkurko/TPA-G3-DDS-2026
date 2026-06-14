package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias;

public enum TipoCategoria {
    COLABORADOR,
    SOSTENEDOR,
    TRANSFORMADOR;

    public TipoCategoria siguiente() {
        return switch (this) {
            case COLABORADOR -> SOSTENEDOR;
            case SOSTENEDOR -> TRANSFORMADOR;
            case TRANSFORMADOR -> TRANSFORMADOR;
        };
    }
}