package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorPersonas;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorNecesidades;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EntidadBeneficiariaService {

    private final GestorEntidadesBeneficiarias gestorEntidades;
    private final GestorPersonas gestorPersonas;
    private final GestorNecesidades gestorNecesidades;

    public EntidadBeneficiariaService(GestorEntidadesBeneficiarias gestorEntidades, GestorPersonas gestorPersonas, GestorNecesidades gestorNecesidades) {
        this.gestorEntidades = gestorEntidades;
        this.gestorPersonas = gestorPersonas;
        this.gestorNecesidades = gestorNecesidades;
    }

    public List<EntidadBeneficiariaDTO> obtenerTodas() {
        return gestorEntidades.listarTodasLasEntidades().stream().map(EntidadBeneficiariaDTO::from).collect(Collectors.toList());
    }

    public EntidadBeneficiariaDTO obtenerEntidadPorId(UUID id) {
        EntidadBeneficiaria entidad = gestorEntidades.obtenerEntidad(id);
        if (entidad == null) throw new IllegalArgumentException("No se encontró la entidad con ID: " + id);
        return EntidadBeneficiariaDTO.from(entidad);
    }

    public EntidadBeneficiariaDTO registrarEntidad(EntidadBeneficiariaDTO dto) {
        EntidadBeneficiaria entidad = dto.toDomain();
        if (entidad.getPersonaJuridica() != null) gestorPersonas.registrarPersona(entidad.getPersonaJuridica());
        gestorEntidades.registrarEntidad(entidad);
        return EntidadBeneficiariaDTO.from(entidad);
    }

    public EntidadBeneficiariaDTO actualizarEntidad(UUID id, EntidadBeneficiariaDTO dto) {
        EntidadBeneficiaria entidadActualizada = dto.toDomain();
        EntidadBeneficiaria existente = gestorEntidades.obtenerEntidad(id);
        if (existente == null) throw new IllegalArgumentException("No se encontró la entidad con ID: " + id);

        if (existente.getPersonaJuridica() != null && entidadActualizada.getPersonaJuridica() != null) {
            gestorPersonas.modificarPersona(existente.getPersonaJuridica().getId(), entidadActualizada.getPersonaJuridica());
        }
        return EntidadBeneficiariaDTO.from(gestorEntidades.modificarEntidad(id, entidadActualizada));
    }

    public void eliminarEntidad(UUID id) {
        gestorEntidades.darDeBajaEntidad(id);
    }

    public List<NecesidadDTO> obtenerNecesidades(UUID idEntidad) {
        EntidadBeneficiaria entidad = gestorEntidades.obtenerEntidad(idEntidad);
        if (entidad == null) throw new IllegalArgumentException("No se encontró la entidad con ID: " + idEntidad);
        return entidad.getNecesidades().stream().map(NecesidadDTO::from).collect(Collectors.toList());
    }

    public NecesidadDTO agregarNecesidad(UUID idEntidad, NecesidadDTO dto) {
        Necesidad necesidad = dto.toDomain();
        gestorNecesidades.crearNecesidad(necesidad);
        gestorEntidades.agregarNecesidadAEntidad(idEntidad, necesidad);
        return NecesidadDTO.from(necesidad);
    }

    public void eliminarNecesidad(UUID idEntidad, UUID idNecesidad) {
        gestorEntidades.eliminarNecesidadDeEntidad(idEntidad, idNecesidad);
        gestorNecesidades.eliminarNecesidad(idNecesidad);
    }

    public List<DonacionDTO> obtenerDonaciones(UUID idEntidad) {
        return gestorEntidades.obtenerDonacionesDeEntidad(idEntidad).stream().map(DonacionDTO::from).collect(Collectors.toList());
    }
}