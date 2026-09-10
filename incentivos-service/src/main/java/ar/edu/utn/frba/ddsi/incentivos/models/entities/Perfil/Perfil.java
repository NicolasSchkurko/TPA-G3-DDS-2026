package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCompletada;
import ar.edu.utn.frba.ddsi.incentivos.models.events.CategoriaNuevaPublicar;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCambiada;
import org.springframework.data.domain.AbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Perfil extends AbstractAggregateRoot<Perfil> {

    private UUID idUsuario; // id en donaciones

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idPerfil; // id interno

    private String nombreUsuario;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoriaActual;

    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InsigniaObtenida> insigniasObtenidas;

    @OneToOne(cascade = CascadeType.ALL)
    private ProgresoMision progresoMisionActual;

    public Perfil(UUID idUsuario, String nombreUsuario) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.categoriaActual = null;
        this.insigniasObtenidas = new ArrayList<>();
        this.progresoMisionActual = null;
    }

    public void verificarProgresoMision(List<ImpactoDonacion> donaciones){
        if (progresoMisionActual != null)
            progresoMisionActual.evaluarConstancia(donaciones, LocalDateTime.now());
    }

    public Boolean progresarMision(ImpactoDonacion donacion,
                                   List<ImpactoDonacion> donaciones){
        if (progresoMisionActual == null) return false;

        Mision misionAnterior = progresoMisionActual.getMision();
        Insignia insignia = progresoMisionActual.progresarMision(donacion, donaciones);

        if (insignia != null) {
            this.insigniasObtenidas.add(new InsigniaObtenida(this, insignia));

            registerEvent(new MisionCompletada(
                    misionAnterior != null ? misionAnterior.getNombreMision() : null,
                    insignia.getNombre(),
                    this.idUsuario,
                    this.nombreUsuario,
                    donacion
            ));

            return true;
        }
        return false;
    }

    public void cambiarMision(Mision misionNueva, Mision misionAnterior, MedioContacto contacto) {
        if (misionNueva == null) {
            this.progresoMisionActual = null;
            return;
        }

        this.progresoMisionActual = new ProgresoMision(misionNueva);

        if (misionAnterior != null) {
            registerEvent(new MisionCambiada(
                    misionAnterior.getNombreMision(),
                    misionAnterior.getInsigniaObjetivo().getNombre(),
                    this.nombreUsuario,
                    this.idUsuario,
                    contacto,
                    misionNueva.getNombreMision()
            ));
        }
    }

    public void cambiarCategoria(Categoria categoriaNueva,
                                 Categoria categoriaAnterior,
                                 Mision misionAnterior,
                                 MedioContacto contacto) {
        this.categoriaActual = categoriaNueva;

        Mision primeraMision = categoriaNueva != null ? categoriaNueva.primeraMision() : null;
        this.progresoMisionActual = primeraMision != null ? new ProgresoMision(primeraMision) : null;

        if (categoriaAnterior != null && categoriaNueva != null) {
            registerEvent(new CategoriaNuevaPublicar(
                    categoriaAnterior.getNombre(),
                    categoriaNueva.getNombre(),
                    this.nombreUsuario,
                    contacto
            ));
        }

        if (misionAnterior != null && primeraMision != null) {
            registerEvent(new MisionCambiada(
                    misionAnterior.getNombreMision(),
                    misionAnterior.getInsigniaObjetivo().getNombre(),
                    this.nombreUsuario,
                    this.idUsuario,
                    contacto,
                    primeraMision.getNombreMision()
            ));
        }
    }
}