package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.Necesidades.NecesidadExtraordinaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.Necesidades.NecesidadRecurrente;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.Necesidades.Necesidad;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EntidadBeneficiariaService {

    private final RepositorioEntidadesBeneficiarias repositorio;

    public EntidadBeneficiariaService(RepositorioEntidadesBeneficiarias repositorio) {
        this.repositorio = repositorio;
    }

    // --- OPERACIONES CRUD ENTIDADES ---

    public List<EntidadBeneficiariaDTO> obtenerTodas() {
        return repositorio.findAll().stream()
                          .map(this::convertirADTO)
                          .collect(Collectors.toList());
    }

    public EntidadBeneficiariaDTO obtenerEntidadPorId(UUID id) {
        return repositorio.findById(id)
                          .map(this::convertirADTO)
                          .orElseThrow(() -> new IllegalArgumentException("No se encontró la entidad con ID: " + id));
    }

    public EntidadBeneficiariaDTO registrarEntidad(EntidadBeneficiariaDTO dto) {
        DireccionDTO dirDTO = dto.getDireccion();

        Pais pais = new Pais(dirDTO.getPais());
        Provincia provincia = new Provincia(dirDTO.getProvincia(), pais);
        Ciudad ciudad = new Ciudad(dirDTO.getCiudad(), provincia);

        Direccion direccion = new Direccion(
            dirDTO.getCalleUno(), dirDTO.getCalleDos(), dirDTO.getAltura(),
            dirDTO.getPiso(), dirDTO.getDepartamento(), ciudad
        );

        EntidadBeneficiaria entidad = new EntidadBeneficiaria(
            dto.getRazonSocial(), direccion, new Telefono(dto.getTelefono()), null
        );

        EntidadBeneficiaria guardada = repositorio.save(entidad);
        return convertirADTO(guardada);
    }

    public EntidadBeneficiariaDTO actualizarEntidad(UUID id, EntidadBeneficiariaDTO dto) {
        EntidadBeneficiaria existente = repositorio.findById(id)
                                                   .orElseThrow(() -> new IllegalArgumentException("No se encontró la entidad con ID: " + id));

        existente.setRazonSocial(dto.getRazonSocial());
        existente.setNroTell(new Telefono(dto.getTelefono()));
        // Actualizar dirección si es necesario...

        return convertirADTO(repositorio.save(existente));
    }

    public void eliminarEntidad(UUID id) {
        repositorio.deleteById(id);
    }

    // --- OPERACIONES CRUD NECESIDADES ---

    public List<NecesidadDTO> obtenerNecesidades(UUID idEntidad) {
        EntidadBeneficiaria entidad = repositorio.findById(idEntidad)
                                                 .orElseThrow(() -> new IllegalArgumentException("No se encontró la entidad"));

        return entidad.getNecesidades().stream()
                      .map(this::convertirNecesidadADTO)
                      .collect(Collectors.toList());
    }

    public NecesidadDTO agregarNecesidad(UUID idEntidad, NecesidadDTO dto) {
        EntidadBeneficiaria entidad = repositorio.findById(idEntidad)
                                                 .orElseThrow(() -> new IllegalArgumentException("No se encontró la entidad"));

        SubcategoriaBien subcategoria = new SubcategoriaBien(dto.getNombreSubcategoria(), new CategoriaBien(dto.getNombreCategoria()));

        Necesidad necesidad = switch (dto.getTipoNecesidad().toUpperCase()) {
            case "RECURRENTE" -> new NecesidadRecurrente(
                subcategoria, dto.getDescripcion(), dto.getCantidadObjetivo(), dto.getPlazoEnDias()
            );
            case "EXTRAORDINARIA" -> new NecesidadExtraordinaria(
                subcategoria, dto.getDescripcion(), dto.getCantidadObjetivo()
            );
            default -> throw new IllegalArgumentException("Tipo de necesidad inválido: " + dto.getTipoNecesidad());
        };

        entidad.agregarNecesidad(necesidad);
        repositorio.save(entidad); // Guardar cambios en el repo

        return convertirNecesidadADTO(necesidad);
    }

    public void eliminarNecesidad(UUID idEntidad, UUID idNecesidad) {
        EntidadBeneficiaria entidad = repositorio.findById(idEntidad)
                                                 .orElseThrow(() -> new IllegalArgumentException("No se encontró la entidad"));

        Necesidad necesidad = entidad.buscarNecesidadPorId(idNecesidad)
                                     .orElseThrow(() -> new IllegalArgumentException("No se encontró la necesidad"));

        entidad.eliminarNecesidad(necesidad);
        repositorio.save(entidad);
    }

    // --- OTROS MÉTODOS ---

    public List<DonacionDTO> obtenerDonaciones(UUID idEntidad) {
        EntidadBeneficiaria entidad = repositorio.findById(idEntidad)
                                                 .orElseThrow(() -> new IllegalArgumentException("No se encontró la entidad"));

        return entidad.verDonaciones().stream()
                      .map(this::convertirDonacionADTO)
                      .collect(Collectors.toList());
    }

    // --- MAPPERS INTERNOS ---

    private EntidadBeneficiariaDTO convertirADTO(EntidadBeneficiaria entidad) {
        EntidadBeneficiariaDTO dto = new EntidadBeneficiariaDTO();
        // Si a futuro añades id a EntidadBeneficiariaDTO, deberías setearlo aquí (dto.setId(entidad.getId());)
        dto.setRazonSocial(entidad.getRazonSocial());
        dto.setTelefono(entidad.getNroTell() != null ? entidad.getNroTell().getNumeroDeTelefono() : null);
        return dto;
    }

    private NecesidadDTO convertirNecesidadADTO(Necesidad necesidad) {
        NecesidadDTO dto = new NecesidadDTO();
        // dto.setId(necesidad.getId()); // Añadir si agregas ID al DTO
        dto.setDescripcion(necesidad.getDescripcion());
        dto.setCantidadObjetivo(necesidad.getCantidadObjetivo());
        dto.setNombreSubcategoria(necesidad.getSubcategoria() != null ? necesidad.getSubcategoria().getNombre() : null);
        dto.setNombreCategoria(necesidad.getSubcategoria() != null && necesidad.getSubcategoria().getCategoria() != null
                               ? necesidad.getSubcategoria().getCategoria().getNombre() : null);
        dto.setTipoNecesidad(necesidad instanceof NecesidadRecurrente ? "RECURRENTE" : "EXTRAORDINARIA");
        if (necesidad instanceof NecesidadRecurrente recurrente) {
            dto.setPlazoEnDias(recurrente.getPlazoEnDias());
        }
        return dto;
    }

    private DonacionDTO convertirDonacionADTO(Donacion donacion) {
        DonacionDTO dto = new DonacionDTO();
        dto.setDonanteName(donacion.getDonante() != null ? donacion.getDonante().darNombre() : "Desconocido");
        dto.setEntidadBeneficiaria(donacion.getEntidad() != null ? donacion.getEntidad().getRazonSocial() : null);
        dto.setDescripcion(donacion.getDescripcion());
        dto.setEstado(donacion.getEstado() != null ? donacion.getEstado().name() : "N/A");

        dto.setSubcategoriaName(donacion.getSubcategoria() != null ? donacion.getSubcategoria().getNombre() : "N/A");
        dto.setCategoriaBienName(donacion.getSubcategoria() != null && donacion.getSubcategoria().getCategoria() != null
                                 ? donacion.getSubcategoria().getCategoria().getNombre() : "N/A");

        dto.setFechaEntrega(donacion.getFechaEntrega());
        dto.setCantidadTotalBienes(donacion.sumaCantidadBienes());
        return dto;
    }
}