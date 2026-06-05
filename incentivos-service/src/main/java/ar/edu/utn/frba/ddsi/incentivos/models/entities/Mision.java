package ar.edu.utn.frba.ddsi.incentivos.models.entities;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class Mision {
    List<Insignia> insignias;
}
