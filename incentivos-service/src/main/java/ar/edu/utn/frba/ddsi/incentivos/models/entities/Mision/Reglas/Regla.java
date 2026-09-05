package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Regla {
    //que sea capaz de hacer una mision tipo:
//hacer x cantidad de x tipo de donaciones por x cant de tiempo
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idRegla;

    @OneToOne(cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "constancia_id")
    private ReglaConstancia constancia; //puede ser null

    @Enumerated(EnumType.STRING)
    private AtributoImpacto atributo; //atributo de ImpactoDonacion

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "operacion_id", nullable = false)
    private Operacion operacion; //define relacion entre atributo y lista donaciones

    public Regla(
            ReglaConstancia constancia,
            AtributoImpacto atributo,
            Operacion operacion
    ) {
        this.idRegla = UUID.randomUUID();
        this.constancia = constancia;
        this.atributo = atributo;
        this.operacion = operacion;
    }

    public Boolean estaCompleta(Integer progreso) {
        return operacion.estaCompleta(progreso);
    }

    public Object aplicar(ImpactoDonacion donacion){
        return switch (atributo) {
            case ESTADO -> donacion.getEstado();
            case CATEGORIA -> donacion.getCategoria();
            case CANTIDAD_BIENES -> donacion.getCantidadBienes();
            case SUBCATEGORIA -> donacion.getSubCategoria();
            case ENTIDAD -> donacion.getEntidadBeneficiaria();
            case FECHA -> donacion.getFechaEntrega();
        };
    }

    public Boolean operar(Object valorAtributo){
        return operacion.calcularProgreso(valorAtributo);
    }
}
