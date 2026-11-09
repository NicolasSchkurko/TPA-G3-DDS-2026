package ar.edu.utn.frba.ddsi.donaciones.services;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ar.edu.utn.frba.ddsi.donaciones.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioCiudades;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioNecesidades;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioPersonas;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioSubcategoriasDeBienes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EntidadBeneficiariaService {

    private final RepositorioPersonas repositorioPersonas;
    private final RepositorioSubcategoriasDeBienes repositorioSubcategoriasDeBienes;
    private final RepositorioCiudades repositorioCiudades;
    private final RepositorioEntidadesBeneficiarias repositorioEntidadesBeneficiarias;
    private final RepositorioNecesidades repositorioNecesidades;
    private final GeocodificacionService geocodificacionService;

    public EntidadBeneficiariaService(RepositorioPersonas repositorioPersonas, RepositorioSubcategoriasDeBienes repositorioSubcategoriasDeBienes, RepositorioCiudades repositorioCiudades, RepositorioEntidadesBeneficiarias repositorioEntidadesBeneficiarias, RepositorioNecesidades repositorioNecesidades, GeocodificacionService geocodificacionService) {
        this.repositorioPersonas = repositorioPersonas;
        this.repositorioSubcategoriasDeBienes = repositorioSubcategoriasDeBienes;
        this.repositorioCiudades = repositorioCiudades;
        this.repositorioEntidadesBeneficiarias = repositorioEntidadesBeneficiarias;
        this.repositorioNecesidades = repositorioNecesidades;
        this.geocodificacionService = geocodificacionService;
    }

    public List<EntidadBeneficiariaDTO> obtenerTodas() {
        return repositorioEntidadesBeneficiarias.obtenerTodas().stream()
                .filter(EntidadBeneficiaria::isActiva)
                .map(EntidadBeneficiariaDTO::from)
                .collect(Collectors.toList());
    }

    public Page<EntidadBeneficiariaDTO> obtenerTodas(Pageable pageable) {
        return repositorioEntidadesBeneficiarias.obtenerTodasActivas(pageable).map(EntidadBeneficiariaDTO::from);
    }

    public EntidadBeneficiariaDTO obtenerEntidadPorId(UUID id) {
        EntidadBeneficiaria entidad = repositorioEntidadesBeneficiarias.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la entidad con ID: " + id));
        return EntidadBeneficiariaDTO.from(entidad);
    }

    @Transactional(rollbackFor = Exception.class)
    public EntidadBeneficiariaDTO registrarEntidad(EntidadBeneficiariaDTO dto) {
        Ciudad ciudad = resolverCiudad(dto.getDireccion());
        EntidadBeneficiaria entidad = dto.toDomain(ciudad);
        geocodificarDireccion(entidad.getDireccion());
        if (entidad.getPersonaJuridica() != null) repositorioPersonas.registrarPersona(entidad.getPersonaJuridica());
        registrarEntidad(entidad);
        return EntidadBeneficiariaDTO.from(entidad);
    }

    @Transactional(rollbackFor = Exception.class)
    public EntidadBeneficiariaDTO actualizarEntidad(UUID id, EntidadBeneficiariaDTO dto) {
        Ciudad ciudad = resolverCiudad(dto.getDireccion());
        EntidadBeneficiaria entidadActualizada = dto.toDomain(ciudad);
        EntidadBeneficiaria existente = repositorioEntidadesBeneficiarias.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la entidad con ID: " + id));
        if (entidadActualizada.getDireccion() != null
                && mismaDireccion(existente.getDireccion(), entidadActualizada.getDireccion())
                && existente.getDireccion().getLatitud() != null
                && existente.getDireccion().getLongitud() != null) {
            entidadActualizada.getDireccion().setLatitud(existente.getDireccion().getLatitud());
            entidadActualizada.getDireccion().setLongitud(existente.getDireccion().getLongitud());
        } else if (entidadActualizada.getDireccion() != null) {
            geocodificarDireccion(entidadActualizada.getDireccion());
        }

        if (existente.getPersonaJuridica() != null && entidadActualizada.getPersonaJuridica() != null) {
            repositorioPersonas.modificarPersona(existente.getPersonaJuridica().getId(), entidadActualizada.getPersonaJuridica());
        }
        return EntidadBeneficiariaDTO.from(repositorioEntidadesBeneficiarias.modificarEntidad(id, entidadActualizada));
    }

    @Transactional
    public EntidadBeneficiariaDTO geocodificarEntidad(UUID id) {
        EntidadBeneficiaria entidad = repositorioEntidadesBeneficiarias.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la entidad con ID: " + id));
        if (entidad.getDireccion() != null
                && (entidad.getDireccion().getLatitud() == null || entidad.getDireccion().getLongitud() == null)) {
            geocodificarDireccion(entidad.getDireccion());
            repositorioEntidadesBeneficiarias.actualizar(id, entidad);
        }
        return EntidadBeneficiariaDTO.from(entidad);
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminarEntidad(UUID id) {
        EntidadBeneficiaria entidad = repositorioEntidadesBeneficiarias.buscarPorId(id).orElse(null);
        if (entidad == null) {
            return;
        }

        entidad.darDeBaja();
        repositorioEntidadesBeneficiarias.actualizar(id, entidad);
        repositorioEntidadesBeneficiarias.flush();
        System.out.println("Entidad beneficiaria dada de baja con éxito.");
    }

    public List<NecesidadDTO> obtenerNecesidades(UUID idEntidad) {
        EntidadBeneficiaria entidad = repositorioEntidadesBeneficiarias.buscarPorId(idEntidad).orElse(null);
        if (entidad == null) throw new IllegalArgumentException("No se encontró la entidad con ID: " + idEntidad);
        return entidad.getNecesidades().stream()
                .filter(Necesidad::isActiva)
                .map(NecesidadDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public NecesidadDTO agregarNecesidad(UUID idEntidad, NecesidadDTO dto) {
        SubcategoriaBien subcategoria = repositorioSubcategoriasDeBienes.obtenerOCrearSubcategoria(dto.getNombreCategoria(), dto.getNombreSubcategoria());
        Necesidad necesidad = dto.toDomain(subcategoria);
        Necesidad necesidadPersistida = crearNecesidad(necesidad);
        repositorioEntidadesBeneficiarias.agregarNecesidadAEntidad(idEntidad, necesidadPersistida);
        return NecesidadDTO.from(necesidadPersistida);
    }

    @Transactional(rollbackFor = Exception.class)
    public NecesidadDTO actualizarNecesidad(UUID idEntidad, UUID idNecesidad, NecesidadDTO dto) {
        // idEntidad antes viajaba sin usarse: PUT /entidades/A/necesidades/idDeB modificaba la
        // necesidad de B igual. Ahora se valida que la necesidad pertenezca a esa entidad.
        EntidadBeneficiaria entidad = repositorioEntidadesBeneficiarias.buscarPorId(idEntidad).orElse(null);
        if (entidad == null) throw new IllegalArgumentException("No se encontró la entidad con ID: " + idEntidad);
        if (entidad.buscarNecesidadPorId(idNecesidad).isEmpty()) {
            throw new IllegalArgumentException("La necesidad " + idNecesidad + " no pertenece a la entidad " + idEntidad);
        }

        SubcategoriaBien subcategoria = repositorioSubcategoriasDeBienes.obtenerOCrearSubcategoria(dto.getNombreCategoria(), dto.getNombreSubcategoria());
        Necesidad necesidadActualizada = dto.toDomain(subcategoria);

        return NecesidadDTO.from(repositorioNecesidades.modificarNecesidad(idNecesidad, necesidadActualizada));
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminarNecesidad(UUID idEntidad, UUID idNecesidad) {
        EntidadBeneficiaria entidad = repositorioEntidadesBeneficiarias.buscarPorId(idEntidad).orElse(null);
        if (entidad == null) {
            throw new IllegalArgumentException("No se encontró la entidad con ID: " + idEntidad);
        }

        Necesidad necesidad = entidad.buscarNecesidadPorId(idNecesidad)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la necesidad con ID: " + idNecesidad));

        necesidad.setActiva(false);
        repositorioNecesidades.actualizar(idNecesidad, necesidad);
        repositorioNecesidades.flush();
        System.out.println("Necesidad dada de baja con éxito.");
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

    private void geocodificarDireccion(Direccion direccion) {
        if (direccion == null) return;
        direccion.setLatitud(null);
        direccion.setLongitud(null);
        geocodificacionService.geocodificar(direccion).ifPresent(coordenadas -> {
            direccion.setLatitud(coordenadas.latitud());
            direccion.setLongitud(coordenadas.longitud());
        });
    }

    private static boolean mismaDireccion(Direccion primera, Direccion segunda) {
        if (primera == null || segunda == null) return primera == segunda;
        return Objects.equals(primera.getCalleUno(), segunda.getCalleUno())
                && Objects.equals(primera.getAltura(), segunda.getAltura())
                && Objects.equals(nombreCiudad(primera), nombreCiudad(segunda))
                && Objects.equals(nombreProvincia(primera), nombreProvincia(segunda))
                && Objects.equals(nombrePais(primera), nombrePais(segunda));
    }

    private static String nombreCiudad(Direccion direccion) {
        return direccion.getCiudad() == null ? null : direccion.getCiudad().getNombre();
    }

    private static String nombreProvincia(Direccion direccion) {
        return direccion.getCiudad() == null || direccion.getCiudad().getProvincia() == null
                ? null : direccion.getCiudad().getProvincia().getNombre();
    }

    private static String nombrePais(Direccion direccion) {
        return direccion.getCiudad() == null || direccion.getCiudad().getProvincia() == null
                || direccion.getCiudad().getProvincia().getPais() == null
                ? null : direccion.getCiudad().getProvincia().getPais().getNombre();
    }

    private Necesidad crearNecesidad(Necesidad nuevoNecesidad) {
        try {
            Necesidad persistida = repositorioNecesidades.guardar(nuevoNecesidad);
            System.out.println("Necesidad registrada con éxito con ID: " + persistida.getId());
            return persistida;
        } catch (IllegalArgumentException e) {
            System.err.println("Error al registrar Necesidad: " + e.getMessage());
            return nuevoNecesidad;
        }
    }
}
