package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;


import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class InsigniaObtenida {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "perfil_id")
  private Perfil perfil;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "insignia_id")
  private Insignia insignia;

  private LocalDateTime fechaObtencion;

  public InsigniaObtenida(Perfil perfil, Insignia insignia) {
    this.perfil = perfil;
    this.insignia = insignia;
    this.fechaObtencion = LocalDateTime.now();
  }

  /**
   * Dos Obtenidas son la misma si son del mismo perfil y de la misma insignia.
   *
   * <p>Hace falta para que {@code Perfil.insigniasObtenidas} sea un {@code Set} y sirva
   * de deduplicación: sin {@code equals}, un {@code Set} de entidades compara por
   * identidad y nunca reconoce dos filas que son la misma insignia (punto 28).
   *
   * <p>La clave es (perfil, insignia) y no el id, justamente para que dos objetos
   * distintos que representan lo mismo se reconozcan.
   */
  @Override
  public boolean equals(Object otro) {
    if (this == otro) return true;
    if (!(otro instanceof InsigniaObtenida otra)) return false;
    return Objects.equals(perfil, otra.perfil)
            && Objects.equals(insignia, otra.insignia);
  }

  @Override
  public int hashCode() {
    return Objects.hash(perfil, insignia);
  }
}
