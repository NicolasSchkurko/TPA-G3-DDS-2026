package ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Setter
@Getter
public class MediosDeContacto {

    @Id
    private UUID id = UUID.randomUUID();

    // Referencia a uno de los medios de la lista de abajo.
    // Cascade PERSIST/MERGE (sin REMOVE) porque suele apuntar a un medio recién creado
    // que todavía no existe en la base (p.ej. al dar de alta una entidad con teléfono predeterminado);
    // el borrado de la fila la maneja la lista de abajo vía orphanRemoval.
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "medio_predeterminado_id")
    private MedioDeContacto medioDeContactoPredeterminado;

    // Se inicializa la lista para evitar el NullPointerException
    // Unidireccional: MedioDeContacto no tiene referencia de vuelta (mismo patrón que EntidadBeneficiaria->Necesidad).
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "medios_de_contacto_id")
    private List<MedioDeContacto> listaMediosDeContacto = new ArrayList<>();

    public MediosDeContacto() {
    }

    public void agregarMedioDeContacto(MedioDeContacto medioDeContacto) {
        this.listaMediosDeContacto.add(medioDeContacto);
    }

    public void agregarMediosDeContacto(List<MedioDeContacto> mediosDeContacto) {
        this.listaMediosDeContacto.addAll(mediosDeContacto);
    }

    public void eliminarMedioDeContacto(MedioDeContacto medioDeContacto) {
        this.listaMediosDeContacto.remove(medioDeContacto);
    }

    public void eliminarMediosDeContacto(List<MedioDeContacto> mediosDeContacto) {
        this.listaMediosDeContacto.removeAll(mediosDeContacto);
    }

    @Override
    public String toString() {
        return "MediosDeContacto{listaMediosDeContacto=" + listaMediosDeContacto + '}';
    }

    public void enviarMensajeAMedios(Mensaje mensaje) {
        listaMediosDeContacto.forEach(medio -> medio.enviarMensaje(mensaje));
    }

}


