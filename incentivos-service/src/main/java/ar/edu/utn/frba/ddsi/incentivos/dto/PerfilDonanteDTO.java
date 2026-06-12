package ar.edu.utn.frba.ddsi.incentivos.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter

public class PerfilDonanteDTO {
    //recibimos de PersonaDonanteDTO-Serv-Donaciones
    private UUID idUsuario;
    private Integer totalDonaciones;
    private String nombreUsuario;
}