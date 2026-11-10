package ar.edu.utn.frba.ddsi.logisticas.dto.entrega;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class DireccionDTO {
    private UUID idEntidad;
    private String calleUno;
    private String calleDos;
    private Integer altura;
    private Integer piso;
    private String departamento;
    private String ciudad;
    private String provincia;
    private String pais;

    public DireccionDTO(UUID idEntidad,
                        String calleUno,
                        String calleDos,
                        Integer altura,
                        Integer piso,
                        String departamento,
                        String ciudad,
                        String provincia,
                        String pais){
        this.idEntidad = idEntidad;
        this.calleUno = calleUno;
        this.calleDos = calleDos;
        this.altura = altura;
        this.piso = piso;
        this.departamento = departamento;
        this.ciudad = ciudad;
        this.provincia = provincia;
        this.pais = pais;
    }

    public DireccionDTO aplicar(Entidad entidad) {
        if (entidad == null || entidad.getDireccionDestino() == null) return null;

        this.idEntidad = entidad.getIdEntidadBeneficiaria();
        Direccion direccion = entidad.getDireccionDestino();

        this.calleUno = direccion.getCalle1();
        this.calleDos = direccion.getCalle2();
        this.altura = direccion.getAltura();
        this.piso = direccion.getPiso();
        this.departamento = direccion.getDepartamento();

        if (direccion.getCiudad() != null) {
            this.ciudad = direccion.getCiudad().getNombre();
            if (direccion.getCiudad().getProvincia() != null) {
                this.provincia = direccion.getCiudad().getProvincia().getNombre();
                if (direccion.getCiudad().getProvincia().getPais() != null) {
                    this.pais = direccion.getCiudad().getProvincia().getPais().getNombre();
                }
            }
        }

        return this;
    }
}