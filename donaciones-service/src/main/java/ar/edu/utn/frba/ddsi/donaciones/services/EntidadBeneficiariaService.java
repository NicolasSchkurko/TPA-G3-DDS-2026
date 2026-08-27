package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadExtraordinaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadRecurrente;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.TipoJuridico;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorNecesidades;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorPersonas;
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

    public EntidadBeneficiariaService(GestorEntidadesBeneficiarias gestorEntidades,
                                      GestorPersonas gestorPersonas,
                                      GestorNecesidades gestorNecesidades) {
        this.gestorEntidades = gestorEntidades;
        this.gestorPersonas = gestorPersonas;
        this.gestorNecesidades = gestorNecesidades;
    }

    public List<EntidadBeneficiariaDTO> obtenerTodas() {
        return gestorEntidades.listarTodasLasEntidades().stream()
                              .map(this::convertirADTO)
                              .collect(Collectors.toList());
    }

    public EntidadBeneficiariaDTO obtenerEntidadPorId(UUID id) {
        EntidadBeneficiaria entidad = gestorEntidades.obtenerEntidad(id);
        if (entidad == null) {
            throw new IllegalArgumentException("No se encontró la entidad con ID: " + id);
        }
        return convertirADTO(entidad);
    }

    public EntidadBeneficiariaDTO registrarEntidad(EntidadBeneficiariaDTO dto) {
        EntidadBeneficiaria entidad = mapearADominio(dto);
        if (entidad.getPersonaJuridica() != null) {
            gestorPersonas.registrarPersona(entidad.getPersonaJuridica());
        }
        gestorEntidades.registrarEntidad(entidad);
        return convertirADTO(entidad);
    }

    public EntidadBeneficiariaDTO actualizarEntidad(UUID id, EntidadBeneficiariaDTO dto) {
        EntidadBeneficiaria entidadActualizada = mapearADominio(dto);
        EntidadBeneficiaria existente = gestorEntidades.obtenerEntidad(id);
        if (existente == null) {
            throw new IllegalArgumentException("No se encontró la entidad con ID: " + id);
        }

        if (existente.getPersonaJuridica() != null && entidadActualizada.getPersonaJuridica() != null) {
            gestorPersonas.modificarPersona(existente.getPersonaJuridica().getId(), entidadActualizada.getPersonaJuridica());
        }

        existente.setDireccion(entidadActualizada.getDireccion());
        gestorEntidades.modificarEntidad(id, existente);

        return convertirADTO(existente);
    }

    public void eliminarEntidad(UUID id) {
        gestorEntidades.darDeBajaEntidad(id);
    }

    public List<NecesidadDTO> obtenerNecesidades(UUID idEntidad) {
        EntidadBeneficiaria entidad = gestorEntidades.obtenerEntidad(idEntidad);
        if (entidad == null) {
            throw new IllegalArgumentException("No se encontró la entidad con ID: " + idEntidad);
        }
        return entidad.getNecesidades().stream()
                      .map(this::convertirNecesidadADTO)
                      .collect(Collectors.toList());
    }

    public NecesidadDTO agregarNecesidad(UUID idEntidad, NecesidadDTO dto) {
        Necesidad necesidad = mapearNecesidadADominio(dto);
        gestorNecesidades.crearNecesidad(necesidad);
        gestorEntidades.agregarNecesidadAEntidad(idEntidad, necesidad);
        return convertirNecesidadADTO(necesidad);
    }

    public void eliminarNecesidad(UUID idEntidad, UUID idNecesidad) {
        gestorEntidades.eliminarNecesidadDeEntidad(idEntidad, idNecesidad);
        gestorNecesidades.eliminarNecesidad(idNecesidad);
    }

    public List<DonacionDTO> obtenerDonaciones(UUID idEntidad) {
        return gestorEntidades.obtenerDonacionesDeEntidad(idEntidad).stream()
                              .map(this::convertirDonacionADTO)
                              .collect(Collectors.toList());
    }

    // --- MAPPERS INTERNOS ---

    private EntidadBeneficiaria mapearADominio(EntidadBeneficiariaDTO dto) {
        DireccionDTO dirDTO = dto.getDireccion();
        Direccion direccion = null;
        if (dirDTO != null) {
            Pais pais = new Pais(dirDTO.getPais() != null ? dirDTO.getPais() : "Argentina");
            Provincia provincia = new Provincia(dirDTO.getProvincia() != null ? dirDTO.getProvincia() : "Buenos Aires", pais);
            Ciudad ciudad = new Ciudad(dirDTO.getCiudad() != null ? dirDTO.getCiudad() : "CABA", provincia);
            direccion = new Direccion(
                dirDTO.getCalleUno(), dirDTO.getCalleDos(), dirDTO.getAltura(),
                dirDTO.getPiso(), dirDTO.getDepartamento(), ciudad
            );
        }

        Juridica juridica = null;
        if (dto.getRazonSocial() != null || dto.getTelefono() != null) {
            juridica = new Juridica(
                dto.getRazonSocial() != null ? dto.getRazonSocial() : "ONG Sin Nombre",
                "ONG",
                TipoJuridico.ONG,
                "00-00000000-0",
                new ArrayList<>(),
                dto.getRazonSocial() != null ? dto.getRazonSocial() : "ONG Sin Nombre"
            );

            if (dto.getTelefono() != null && !dto.getTelefono().isEmpty()) {
                Telefono tel = new Telefono(dto.getTelefono());
                juridica.agregarMedioDeContacto(tel);
                juridica.getMediosDeContacto().setMedioDeContactoPredeterminado(tel);
            }
        }

        return new EntidadBeneficiaria(direccion, juridica);
    }

    private EntidadBeneficiariaDTO convertirADTO(EntidadBeneficiaria entidad) {
        if (entidad == null) return null;
        EntidadBeneficiariaDTO dto = new EntidadBeneficiariaDTO();

        if (entidad.getPersonaJuridica() != null) {
            dto.setRazonSocial(entidad.getPersonaJuridica().getRazonSocial());

            if (entidad.getPersonaJuridica().getMediosDeContacto() != null &&
                entidad.getPersonaJuridica().getMediosDeContacto().getListaMediosDeContacto() != null) {

                String telefono = entidad.getPersonaJuridica().getMediosDeContacto().getListaMediosDeContacto()
                                         .stream()
                                         .filter(m -> m instanceof Telefono)
                                         .map(MedioDeContacto::getValor)
                                         .findFirst()
                                         .orElse(null);
                dto.setTelefono(telefono);
            }
        }

        return dto;
    }

    private Necesidad mapearNecesidadADominio(NecesidadDTO dto) {
        CategoriaBien categoria = new CategoriaBien(dto.getNombreCategoria() != null ? dto.getNombreCategoria() : "General");
        SubcategoriaBien subcategoria = new SubcategoriaBien(dto.getNombreSubcategoria() != null ? dto.getNombreSubcategoria() : "General", categoria);

        String tipo = dto.getTipoNecesidad() != null ? dto.getTipoNecesidad().toUpperCase() : "RECURRENTE";
        return switch (tipo) {
            case "EXTRAORDINARIA" -> new NecesidadExtraordinaria(
                subcategoria, dto.getDescripcion(), dto.getCantidadObjetivo()
            );
            default -> new NecesidadRecurrente(
                subcategoria, dto.getDescripcion(), dto.getCantidadObjetivo(), dto.getPlazoEnDias() != null ? dto.getPlazoEnDias() : 30
            );
        };
    }

    private NecesidadDTO convertirNecesidadADTO(Necesidad necesidad) {
        if (necesidad == null) return null;
        NecesidadDTO dto = new NecesidadDTO();
        dto.setDescripcion(necesidad.getDescripcion());
        dto.setCantidadObjetivo(necesidad.getCantidadObjetivo());

        if (necesidad.getSubcategoria() != null) {
            dto.setNombreSubcategoria(necesidad.getSubcategoria().getNombre());
            if (necesidad.getSubcategoria().getCategoria() != null) {
                dto.setNombreCategoria(necesidad.getSubcategoria().getCategoria().getNombre());
            }
        }

        dto.setTipoNecesidad(necesidad instanceof NecesidadExtraordinaria ? "EXTRAORDINARIA" : "RECURRENTE");
        if (necesidad instanceof NecesidadRecurrente recurrente) {
            dto.setPlazoEnDias(recurrente.getPlazoEnDias());
        }
        return dto;
    }

    private DonacionDTO convertirDonacionADTO(Donacion donacion) {
        if (donacion == null) return null;
        DonacionDTO dto = new DonacionDTO();

        dto.setDonanteName(donacion.getDonante() != null && donacion.getDonante().getPersona() != null
                           ? donacion.getDonante().getPersona().getNombreDeUsuario() : "Desconocido");

        if (donacion.getEntidad() != null && donacion.getEntidad().getPersonaJuridica() != null) {
            dto.setEntidadBeneficiaria(donacion.getEntidad().getPersonaJuridica().getRazonSocial());
        } else {
            dto.setEntidadBeneficiaria("No asignada");
        }

        dto.setDescripcion(donacion.getDescripcion());
        dto.setEstado(donacion.getEstado() != null ? donacion.getEstado().name() : "N/A");
        dto.setSubcategoriaName(donacion.getSubcategoria() != null ? donacion.getSubcategoria().getNombre() : "N/A");
        dto.setCategoriaBienName(donacion.getSubcategoria() != null && donacion.getSubcategoria().getCategoria() != null
                                 ? donacion.getSubcategoria().getCategoria().getNombre() : "N/A");
        dto.setFechaEntrega(donacion.getFechaEntrega());
        dto.setCantidadTotalBienes(donacion.sumaCantidadBienes());
        dto.setBienes(new ArrayList<>());

        return dto;
    }
}