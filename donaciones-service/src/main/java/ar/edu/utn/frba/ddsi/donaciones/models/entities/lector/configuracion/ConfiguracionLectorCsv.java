package ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.configuracion;

import ar.edu.utn.frba.dds.model.hecho.Hecho;
import ar.edu.utn.frba.dds.model.lector.Lector;
import ar.edu.utn.frba.dds.model.lector.csv.LectorCSV;
import ar.edu.utn.frba.dds.model.lector.csv.MapeoCSV;
import ar.edu.utn.frba.dds.model.lector.csv.filaconverter.FilaConverter;
import ar.edu.utn.frba.dds.model.lector.csv.filaconverter.HechoFilaConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Entidad de configuración para un LectorCSV.
 */
//@Entity
//@DiscriminatorValue("CSV")
public class ConfiguracionLectorCsv extends ConfiguracionLector {

  private char separador;
  private String formatoFecha;

  //  @OneToMany...
  private List<MapeoCSV> mapeos = new ArrayList<>();

  public ConfiguracionLectorCsv() {
  }

  /**
   * Constructor completo usado por el Controller.
   * Convierte el Map simple a la estructura interna List<MapeoCSV>.
   */
  public ConfiguracionLectorCsv(char separador, String formatoFecha, Map<String, List<String>> mapeoColumnas) {
    this.separador = separador;
    this.formatoFecha = formatoFecha;
    if (mapeoColumnas != null) {
      this.mapeos = mapeoColumnas.entrySet()
                                 .stream()
                                 .map(entry -> new MapeoCSV(entry.getKey(), entry.getValue()))
                                 .collect(Collectors.toList());
    }
  }

  @Override
  public <T> Lector<T> build(Class<T> clazz) {
    if (clazz.equals(Hecho.class)) {
      // 1. Convertir la lista interna de MapeoCSV a un mapa simple para el converter.
      Map<String, List<String>> mapeoParaConverter = this.mapeos.stream()
                                                                .collect(Collectors.toMap(
                                                                    MapeoCSV::getCampo,
                                                                    MapeoCSV::getNombresColumnas
                                                                ));

      // 2. Crear el FilaConverter con el mapa y formato.
      FilaConverter<Hecho> converter = new HechoFilaConverter(formatoFecha, mapeoParaConverter);

      // 3. Crear y devolver el LectorCSV.
      @SuppressWarnings("unchecked")
      Lector<T> lector = (Lector<T>) new LectorCSV<>(separador, converter);
      return lector;
    }

    throw new IllegalArgumentException("No hay un FilaConverter definido para la clase: " + clazz.getName());
  }
}