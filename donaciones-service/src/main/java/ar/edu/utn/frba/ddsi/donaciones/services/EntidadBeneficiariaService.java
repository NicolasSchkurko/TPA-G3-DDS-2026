package ar.edu.utn.frba.ddsi.donaciones.services;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ar.edu.utn.frba.ddsi.donaciones.dto.direccion.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorCatalogo;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorGeoref;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeNecesidades;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDePersonas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EntidadBeneficiariaService {

    private final RepositorioDePersonas repositorioPersonas;
    private final GestorCatalogo gestorCatalogo;
    private final RepositorioDeEntidadesBeneficiarias repositorioEntidadesBeneficiarias;
    private final RepositorioDeNecesidades repositorioNecesidades;
    private final GestorGeoref gestorGeoref;

    /** Constructor de conveniencia para tests que no ejercitan geocoding. */
    public EntidadBeneficiariaService(RepositorioDePersonas repositorioPersonas, GestorCatalogo gestorCatalogo, RepositorioDeEntidadesBeneficiarias repositorioEntidadesBeneficiarias, RepositorioDeNecesidades repositorioNecesidades) {
        this(repositorioPersonas, gestorCatalogo,
                repositorioEntidadesBeneficiarias, repositorioNecesidades, new GestorGeoref(null, false));
    }

    @Autowired
    public EntidadBeneficiariaService(RepositorioDePersonas repositorioPersonas, GestorCatalogo gestorCatalogo, RepositorioDeEntidadesBeneficiarias repositorioEntidadesBeneficiarias, RepositorioDeNecesidades repositorioNecesidades, GestorGeoref gestorGeoref) {
        this.repositorioPersonas = repositorioPersonas;
        this.gestorCatalogo = gestorCatalogo;
        this.repositorioEntidadesBeneficiarias = repositorioEntidadesBeneficiarias;
        this.repositorioNecesidades = repositorioNecesidades;
        this.gestorGeoref = gestorGeoref;
    }

    @Transactional(readOnly = true)
    public List<EntidadBeneficiariaDTO> obtenerTodas() {
        return repositorioEntidadesBeneficiarias.obtenerTodas().stream()
                .filter(EntidadBeneficiaria::isActiva)
                .map(EntidadBeneficiariaDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<EntidadBeneficiariaDTO> obtenerTodas(Pageable pageable) {
        return repositorioEntidadesBeneficiarias.obtenerTodasActivas(pageable).map(EntidadBeneficiariaDTO::from);
    }

    @Transactional(readOnly = true)
    public EntidadBeneficiariaDTO obtenerEntidadPorId(UUID id) {
        EntidadBeneficiaria entidad = repositorioEntidadesBeneficiarias.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la entidad con ID: " + id));
        return EntidadBeneficiariaDTO.from(entidad);
    }

    @Transactional(rollbackFor = Exception.class)
    public EntidadBeneficiariaDTO registrarEntidad(EntidadBeneficiariaDTO dto) {
        Ciudad ciudad = resolverCiudad(dto.getDireccion());
        EntidadBeneficiaria entidad = dto.toDomain(ciudad);
        if (dto.getKeycloakSub() != null && !dto.getKeycloakSub().isBlank()) {
            String sub = dto.getKeycloakSub().trim();
            repositorioEntidadesBeneficiarias.buscarPorKeycloakSub(sub)
                    .ifPresent(e -> { throw new IllegalArgumentException(
                        "Esa cuenta ya está vinculada a una entidad beneficiaria. Iniciá sesión y agregá el rol desde tu cuenta."); });
            entidad.setKeycloakSub(sub);
        }
        gestorGeoref.geocodificarAlta(entidad.getDireccion());
        if (entidad.getPersonaJuridica() != null) repositorioPersonas.registrarPersona(entidad.getPersonaJuridica());
        registrarEntidad(entidad);
        return EntidadBeneficiariaDTO.from(entidad);
    }

    @Transactional(rollbackFor = Exception.class)
    public EntidadBeneficiariaDTO actualizarEntidad(UUID id, EntidadBeneficiariaDTO dto) {
        EntidadBeneficiaria existente = repositorioEntidadesBeneficiarias.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la entidad con ID: " + id));

        Ciudad ciudad = resolverCiudad(dto.getDireccion());
        EntidadBeneficiaria entidadActualizada = dto.toDomain(ciudad);
        gestorGeoref.geocodificarActualizacion(entidadActualizada.getDireccion(), existente.getDireccion());

        if (existente.getPersonaJuridica() != null && entidadActualizada.getPersonaJuridica() != null) {
            repositorioPersonas.modificarPersona(existente.getPersonaJuridica().getId(), entidadActualizada.getPersonaJuridica());
        }
        return EntidadBeneficiariaDTO.from(repositorioEntidadesBeneficiarias.modificarEntidad(id, entidadActualizada));
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

    @Transactional(readOnly = true)
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
        SubcategoriaBien subcategoria = gestorCatalogo.obtenerOCrearSubcategoria(dto.getNombreCategoria(), dto.getNombreSubcategoria());
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

        SubcategoriaBien subcategoria = gestorCatalogo.obtenerOCrearSubcategoria(dto.getNombreCategoria(), dto.getNombreSubcategoria());
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

    @Transactional(readOnly = true)
    public List<DonacionResumenDTO> obtenerDonaciones(UUID idEntidad) {
        return obtenerDonacionesDeEntidad(idEntidad).stream().map(DonacionResumenDTO::from).collect(Collectors.toList());
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
        EntidadBeneficiaria entidad = repositorioEntidadesBeneficiarias.buscarPorId(idEntidad)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la entidad con ID: " + idEntidad));
        return entidad.verDonaciones();
    }

    // Resuelve (o crea) la Ciudad/Provincia/Pais del catálogo geográfico compartido ANTES de
    // construir el domain object, para evitar que Direccion apunte a una Ciudad nunca persistida
    // (eso rompía merge() con EntityNotFoundException, ver RepositorioDeCiudades).
    private Ciudad resolverCiudad(DireccionDTO direccionDTO) {
        if (direccionDTO == null) return null;
        return gestorCatalogo.obtenerOCrearCiudad(direccionDTO.getPais(), direccionDTO.getProvincia(), direccionDTO.getCiudad());
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
