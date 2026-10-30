package ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class MedioDeContacto {

    @Id
    private UUID id = UUID.randomUUID();

    public MedioDeContacto(List<TipoDeMensaje> tiposDeMensajeProhibidos) {
    }

    public MedioDeContacto() {
    }

    public abstract String getValor();

    public abstract String getTipo();

    public void enviarMensaje(Mensaje mensaje) {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedioDeContacto that)) return false;
        String thisTipo = getTipo();
        String thatTipo = that.getTipo();
        String thisValor = getValor();
        String thatValor = that.getValor();
        return Objects.equals(thisTipo != null ? thisTipo.toUpperCase() : null,
                              thatTipo != null ? thatTipo.toUpperCase() : null)
            && Objects.equals(thisValor, thatValor);
    }

    @Override
    public int hashCode() {
        String tipo = getTipo();
        return Objects.hash(tipo != null ? tipo.toUpperCase() : null, getValor());
    }
}
