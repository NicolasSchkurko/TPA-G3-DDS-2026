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
        registrarEntidad(entidad);
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
        crearNecesidad(necesidad);
        agregarNecesidadAEntidad(idEntidad, necesidad);
        return NecesidadDTO.from(necesidad);
    }

    public NecesidadDTO actualizarNecesidad(UUID id, NecesidadDTO dto) {
        Necesidad necesidadActualizada = dto.toDomain();
        Necesidad existente = repositorioNecesidades.buscarPorId(id).orElse(null);
        if (existente == null) throw new IllegalArgumentException("No se encontró la necesidad con ID: " + id);

        return NecesidadDTO.from(gestorNecesidades.modificarNecesidad(id, necesidadActualizada));
    }

    public void eliminarNecesidad(UUID idEntidad, UUID idNecesidad) {
        eliminarNecesidadDeEntidad(idEntidad, idNecesidad);
        repositorioNecesidades.eliminarPorId(idNecesidad);
        System.out.println("Administrador dado de baja (si existía).");
    }

    public List<DonacionDTO> obtenerDonaciones(UUID idEntidad) {
        return obtenerDonacionesDeEntidad(idEntidad).stream().map(DonacionDTO::from).collect(Collectors.toList());
    }

    private void registrarEntidad(EntidadBeneficiaria nuevaEntidad) {
        try {
            repositorioEntidadesBeneficiarias.guardar(nuevaEntidad);
            System.out.println("Entidad beneficiaria registrada con éxito con ID: " + nuevaEntidad.getId());
        } catch (IllegalArgumentException e) {
            System.err.println("Error al registrar entidad: " + e.getMessage());
        }
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

    private void agregarNecesidadAEntidad(UUID idEntidad, Necesidad nuevaNecesidad) {
        EntidadBeneficiaria entidad = repositorioEntidadesBeneficiarias.buscarPorId(idEntidad).orElse(null);
        if (entidad != null) {
            entidad.agregarNecesidad(nuevaNecesidad);
            repositorioEntidadesBeneficiarias.actualizar(idEntidad, entidad);
            System.out.println("Necesidad agregada a la entidad: " + idEntidad);
        } else {
            throw new IllegalArgumentException("No se pudo agregar necesidad: Entidad no encontrada.");
        }
    }

    private void eliminarNecesidadDeEntidad(UUID idEntidad, UUID idNecesidad) {
        EntidadBeneficiaria entidad = repositorioEntidadesBeneficiarias.buscarPorId(idEntidad).orElse(null);
        if (entidad == null) {
            throw new IllegalArgumentException("No se encontró la entidad con ID: " + idEntidad);
        }

        Necesidad necesidad = entidad.buscarNecesidadPorId(idNecesidad)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la necesidad con ID: " + idNecesidad));

        entidad.eliminarNecesidad(necesidad);
        repositorioEntidadesBeneficiarias.actualizar(idEntidad, entidad);
        System.out.println("Necesidad desvinculada de la entidad con éxito.");
    }

    private void crearNecesidad(Necesidad nuevoNecesidad) {
        try {
            repositorioNecesidades.guardar(nuevoNecesidad);
            System.out.println("Necesidad registrada con éxito con ID: " + nuevoNecesidad.getId());
        } catch (IllegalArgumentException e) {
            System.err.println("Error al registrar Necesidad: " + e.getMessage());
        }
    }
}