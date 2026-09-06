package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioCiudades;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioNecesidades;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioPersonas;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioSubcategoriasDeBienes;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EntidadBeneficiariaService {

    private final RepositorioPersonas repositorioPersonas;
    private final RepositorioSubcategoriasDeBienes repositorioSubcategoriasDeBienes;
    private final RepositorioCiudades repositorioCiudades;
    private final RepositorioEntidadesBeneficiarias repositorioEntidadesBeneficiarias;
    private final RepositorioNecesidades repositorioNecesidades;

    public EntidadBeneficiariaService(RepositorioPersonas repositorioPersonas, RepositorioSubcategoriasDeBienes repositorioSubcategoriasDeBienes, RepositorioCiudades repositorioCiudades, RepositorioEntidadesBeneficiarias repositorioEntidadesBeneficiarias, RepositorioNecesidades repositorioNecesidades) {
        this.repositorioPersonas = repositorioPersonas;
        this.repositorioSubcategoriasDeBienes = repositorioSubcategoriasDeBienes;
        this.repositorioCiudades = repositorioCiudades;
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
        Ciudad ciudad = resolverCiudad(dto.getDireccion());
        EntidadBeneficiaria entidad = dto.toDomain(ciudad);
        if (entidad.getPersonaJuridica() != null) repositorioPersonas.registrarPersona(entidad.getPersonaJuridica());
        registrarEntidad(entidad);
        return EntidadBeneficiariaDTO.from(entidad);
    }

    public EntidadBeneficiariaDTO actualizarEntidad(UUID id, EntidadBeneficiariaDTO dto) {
        Ciudad ciudad = resolverCiudad(dto.getDireccion());
        EntidadBeneficiaria entidadActualizada = dto.toDomain(ciudad);
        EntidadBeneficiaria existente = repositorioEntidadesBeneficiarias.buscarPorId(id).orElse(null);
        if (existente == null) throw new IllegalArgumentException("No se encontró la entidad con ID: " + id);

        if (existente.getPersonaJuridica() != null && entidadActualizada.getPersonaJuridica() != null) {
            repositorioPersonas.modificarPersona(existente.getPersonaJuridica().getId(), entidadActualizada.getPersonaJuridica());
        }
        return EntidadBeneficiariaDTO.from(repositorioEntidadesBeneficiarias.modificarEntidad(id, entidadActualizada));
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
        SubcategoriaBien subcategoria = repositorioSubcategoriasDeBienes.obtenerOCrearSubcategoria(dto.getNombreCategoria(), dto.getNombreSubcategoria());
        Necesidad necesidad = dto.toDomain(subcategoria);
        crearNecesidad(necesidad);
        repositorioEntidadesBeneficiarias.agregarNecesidadAEntidad(idEntidad, necesidad);
        return NecesidadDTO.from(necesidad);
    }

    public NecesidadDTO actualizarNecesidad(UUID id, NecesidadDTO dto) {
        SubcategoriaBien subcategoria = repositorioSubcategoriasDeBienes.obtenerOCrearSubcategoria(dto.getNombreCategoria(), dto.getNombreSubcategoria());
        Necesidad necesidadActualizada = dto.toDomain(subcategoria);
        Necesidad existente = repositorioNecesidades.buscarPorId(id).orElse(null);
        if (existente == null) throw new IllegalArgumentException("No se encontró la necesidad con ID: " + id);

        return NecesidadDTO.from(repositorioNecesidades.modificarNecesidad(id, necesidadActualizada));
    }

    public void eliminarNecesidad(UUID idEntidad, UUID idNecesidad) {
        repositorioEntidadesBeneficiarias.eliminarNecesidadDeEntidad(idEntidad, idNecesidad);
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

    // Resuelve (o crea) la Ciudad/Provincia/Pais del catálogo geográfico compartido ANTES de
    // construir el domain object, para evitar que Direccion apunte a una Ciudad nunca persistida
    // (eso rompía merge() con EntityNotFoundException, ver RepositorioCiudades).
    private Ciudad resolverCiudad(DireccionDTO direccionDTO) {
        if (direccionDTO == null) return null;
        return repositorioCiudades.obtenerOCrearCiudad(direccionDTO.getPais(), direccionDTO.getProvincia(), direccionDTO.getCiudad());
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