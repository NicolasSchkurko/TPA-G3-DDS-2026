package ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "secuencias")
public class SecuenciaDocumento {

    @Id
    private String id;

    private Long valor;
}