package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorPersonas;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorNecesidades;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioNecesidades;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EntidadBeneficiariaService {

    private final GestorEntidadesBeneficiarias gestorEntidades;
    private final GestorPersonas gestorPersonas;
    private final GestorNecesidades gestorNecesidades;
    private final RepositorioEntidadesBeneficiarias repositorioEntidadesBeneficiarias;
    private final RepositorioNecesidades repositorioNecesidades;

    public EntidadBeneficiariaService(GestorEntidadesBeneficiarias gestorEntidades, GestorPersonas gestorPersonas, GestorNecesidades gestorNecesidades, RepositorioEntidadesBeneficiarias repositorioEntidadesBeneficiarias, RepositorioNecesidades repositorioNecesidades) {
        this.gestorEntidades = gestorEntidades;
        this.gestorPersonas = gestorPersonas;
        this.gestorNecesidades = gestorNecesidades;
        this.repositorioEntidadesBeneficiarias = repositorioEntidadesBeneficiarias;
        this.repositorioNecesidades = repositorioNecesidades;
    }

    public List<EntidadBeneficiariaDTO> obtenerTodas() {
        return repositorioEntidadesBeneficiarias.obtenerTodas().stream().map(EntidadBeneficiariaDTO::from).collect(Collectors.toList());
    }

    public EntidadBeneficiariaDTO obtenerEntidadPorId(UUID id) {
        EntidadBeneficiaria entidad = repositorioEntidadesBeneficiarias.buscarPorId(id).orElse(null);
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
        EntidadBeneficiaria existente = repositorioEntidadesBeneficiarias.buscarPorId(id).orElse(null);
        if (existente == null) throw new IllegalArgumentException("No se encontró la entidad con ID: " + id);

        if (existente.getPersonaJuridica() != null && entidadActualizada.getPersonaJuridica() != null) {
            gestorPersonas.modificarPersona(existente.getPersonaJuridica().getId(), entidadActualizada.getPersonaJuridica());
        }
        return EntidadBeneficiariaDTO.from(gestorEntidades.modificarEntidad(id, entidadActualizada));
    }

    public void eliminarEntidad(UUID id) {
        repositorioEntidadesBeneficiarias.eliminarPorId(id);
        System.out.println("Entidad beneficiaria dada de baja (si existía).");
    }

    public List<NecesidadDTO> obtenerNecesidades(UUID idEntidad) {
        EntidadBeneficiaria entidad = repositorioEntidadesBeneficiarias.buscarPorId(idEntidad).orElse(null);
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
        repositorioNecesidades.eliminarPorId(idNecesidad);
        System.out.println("Administrador dado de baja (si existía).");
    }

    public List<DonacionDTO> obtenerDonaciones(UUID idEntidad) {
        return obtenerDonacionesDeEntidad(idEntidad).stream().map(DonacionDTO::from).collect(Collectors.toList());
    }

    private List<Donacion> obtenerDonacionesDeEntidad(UUID idEntidad) {
        EntidadBeneficiaria entidad = repositorioEntidadesBeneficiarias.buscarPorId(idEntidad).orElse(null);
        if (entidad != null) {
            return entidad.verDonaciones();
        } else {
            System.err.println("Entidad no encontrada.");
            return new ArrayList<>();
        }
    }
}