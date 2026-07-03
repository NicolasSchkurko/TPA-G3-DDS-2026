package ar.edu.utn.frba.ddsi.donaciones.mappers;

import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.RepresentanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Humano;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Representante;

import java.util.ArrayList;
import java.util.List;

public interface RepresentanteDTOToObject {
    static Representante convertirEnObjeto(RepresentanteDTO representanteDTO) {
        Humano humano = new Humano(representanteDTO.getNombre(), representanteDTO.getApellido(), representanteDTO.getEdad(), representanteDTO.getNumeroDeDocumento(),
                Genero.valueOf(representanteDTO.getGenero()));

        Representante representante = new Representante(humano, true);
        return representante;
    }
    static List<Representante> convertirEnObjeto(List<RepresentanteDTO> representantesDTO){
        List<Representante> representantes = new ArrayList<>();
        representantesDTO.forEach(representanteDTO -> representantes.add(convertirEnObjeto(representanteDTO)));
        return representantes;
    }
}
