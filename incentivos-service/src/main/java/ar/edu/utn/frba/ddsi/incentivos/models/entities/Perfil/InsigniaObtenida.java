package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;


import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
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
}