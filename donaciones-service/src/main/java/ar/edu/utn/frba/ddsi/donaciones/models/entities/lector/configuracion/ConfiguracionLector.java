package ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.configuracion;

import ar.edu.utn.frba.dds.model.lector.Lector;

//@Entity
//@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
//@DiscriminatorColumn(name = "tipo_lector")
public abstract class ConfiguracionLector {

//  @Id
//  @GeneratedValue
//  private Long id;

  /**
   * Actúa como una fabric para construir el objeto Lector<T> lógico y genérico.
   *
   * @param clazz La clase del objeto que se espera leer (ej. Hecho.class).
   */
  public abstract <T> Lector<T> build(Class<T> clazz);

//  public Long getId() {
//    return id;
//  }
}