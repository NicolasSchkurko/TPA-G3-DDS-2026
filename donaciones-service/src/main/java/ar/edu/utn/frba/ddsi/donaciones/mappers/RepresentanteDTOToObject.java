package ar.edu.utn.frba.ddsi.donaciones.mappers;

import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.RepresentanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Representante;

import java.util.ArrayList;
import java.util.List;

public interface RepresentanteDTOToObject {
    static Representante convertirEnObjeto(RepresentanteDTO representanteDTO) {
        Humana humana = new Humana(representanteDTO.getNombre(), representanteDTO.getApellido(),
                representanteDTO.getEdad(), representanteDTO.getNumeroDeDocumento(),
                                   Genero.valueOf(representanteDTO.getGenero()), representanteDTO.getNombre());

        Representante representante = new Representante(humana, true);
        return representante;
    }
    static List<Representante> convertirEnObjeto(List<RepresentanteDTO> representantesDTO){
        List<Representante> representantes = new ArrayList<>();
        representantesDTO.forEach(representanteDTO -> representantes.add(convertirEnObjeto(representanteDTO)));
        return representantes;
    }
}
