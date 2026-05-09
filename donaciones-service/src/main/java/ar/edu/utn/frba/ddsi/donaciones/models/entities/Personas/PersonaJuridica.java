package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.lugares.Direccion;

import java.util.List;

public class PersonaJuridica extends PersonaDonante{
    private Direccion direccion;
    private String razonSocial;
    private String rubro;
    private TipoJuridico tipoJuridico;
    private List <Representante> representantes;
}
