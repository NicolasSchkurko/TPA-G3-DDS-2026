package ar.edu.utn.frba.ddsi.front.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** DTO de lectura para las entidades devueltas por donaciones-service. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntidadBeneficiariaDTO {
    private String razonSocial;
    private String rubro;
    private String cuit;
    private String tipoJuridico;
    private String telefono;
    /** Email canónico de la entidad (igual que lo que manda donaciones-service). */
    private String email;
    /** Alias de compatibilidad: apunta al mismo Mail que {@link #email}. */
    private String correoElectronico;
    private DireccionDTO direccion;
    private UUID id;
    private Boolean activa;
    private List<NecesidadDTO> necesidades = new ArrayList<>();

    public List<NecesidadDTO> necesidadesActivas() {
        if (necesidades == null) return List.of();
        return necesidades.stream().filter(n -> n.getActiva() == null || n.getActiva()).toList();
    }
}
