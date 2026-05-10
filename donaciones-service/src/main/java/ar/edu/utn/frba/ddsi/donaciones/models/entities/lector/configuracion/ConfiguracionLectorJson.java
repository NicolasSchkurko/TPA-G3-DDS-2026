package ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.configuracion;

import ar.edu.utn.frba.dds.model.hecho.Hecho;
import ar.edu.utn.frba.dds.model.lector.Lector;
import ar.edu.utn.frba.dds.model.lector.json.LectorJson;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;

//@Entity
//@DiscriminatorValue("JSON")
public class ConfiguracionLectorJson extends ConfiguracionLector {

  @Override
  public <T> Lector<T> build(Class<T> clazz) {
    // La creación de un LectorJson genérico requiere un TypeReference específico.
    if (clazz.equals(Hecho.class)) {
      // El cast es seguro porque está validado por el if.
      // Creamos el TypeReference explícito para List<Hecho>
      @SuppressWarnings("unchecked")
      Lector<T> lector = (Lector<T>) new LectorJson<>(new TypeReference<List<Hecho>>() {});
      return lector;
    }

    // Si en el futuro agregas más tipos importables, agrega los 'else if' aquí.

    throw new IllegalArgumentException("No hay un TypeReference de LectorJson definido para la clase: " + clazz.getName());
  }
}