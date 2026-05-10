package ar.edu.utn.frba.ddsi.donaciones.models.entities.lector;

import java.io.InputStream;
import java.util.List;

public interface Lector<T> {

  List<T> importar(InputStream contenido);
}
