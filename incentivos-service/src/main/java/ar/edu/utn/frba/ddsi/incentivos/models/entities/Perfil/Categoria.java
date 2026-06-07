package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Categoria {

    private TipoCategoria nombre;
    private Categoria siguienteCategoria;
    private List<Mision> misiones;

import java.util.List;

public abstract class Categoria {
    List<Mision> misiones;
    Integer requerimiento;
}
