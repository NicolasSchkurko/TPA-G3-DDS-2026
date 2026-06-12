package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadExtraordinaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadRecurrente;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EntidadBeneficiariaService {

    private final RepositorioEntidadesBeneficiarias repositorio;

    public EntidadBeneficiariaService(RepositorioEntidadesBeneficiarias repositorio) {
        this.repositorio = repositorio;
    }

    public void registrarEntidad(EntidadBeneficiariaDTO dto) {
        DireccionDTO dirDTO = dto.getDireccion();

        Pais pais = new Pais(dirDTO.getPais());
        Provincia provincia = new Provincia(dirDTO.getProvincia(), pais);
        Ciudad ciudad = new Ciudad(dirDTO.getCiudad(), provincia);

        Direccion direccion = new Direccion(
                dirDTO.getCalleUno(),
                dirDTO.getCalleDos(),
                dirDTO.getAltura(),
                dirDTO.getPiso(),
                dirDTO.getDepartamento(),
                ciudad
        );

        EntidadBeneficiaria entidad = new EntidadBeneficiaria(
                dto.getRazonSocial(),
                direccion,
                new Telefono(dto.getTelefono()),
                null  // correos pendiente
        );
        repositorio.agregarEntidad(entidad);
    }

    public List<EntidadBeneficiariaDTO> obtenerTodas() {
        return repositorio.obtenerTodas().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    public EntidadBeneficiariaDTO obtenerEntidad(String razonSocial) {
        return convertirADTO(repositorio.buscarPorRazonSocial(razonSocial));
    }

    public List<NecesidadDTO> obtenerNecesidades(String razonSocial) {
        return repositorio.buscarPorRazonSocial(razonSocial).getNecesidades().stream()
                .map(this::convertirNecesidadADTO)
                .collect(Collectors.toList());
    }

    public List<DonacionDTO> obtenerDonaciones(String razonSocial) {
        return repositorio.buscarPorRazonSocial(razonSocial).verDonaciones().stream()
                .map(this::convertirDonacionADTO)
                .collect(Collectors.toList());
    }

    private EntidadBeneficiariaDTO convertirADTO(EntidadBeneficiaria entidad) {
        EntidadBeneficiariaDTO dto = new EntidadBeneficiariaDTO();
        dto.setRazonSocial(entidad.getRazonSocial());
        dto.setTelefono(entidad.getNroTell().getNumeroDeTelefono());
        return dto;
    }

    private NecesidadDTO convertirNecesidadADTO(Necesidad necesidad) {
        NecesidadDTO dto = new NecesidadDTO();
        dto.setDescripcion(necesidad.getDescripcion());
        dto.setCantidadObjetivo(necesidad.getCantidadObjetivo());
        dto.setNombreSubcategoria(necesidad.getSubcategoria().getNombre());
        dto.setNombreCategoria(necesidad.getSubcategoria().getCategoria().getNombre());
        dto.setTipoNecesidad(necesidad instanceof NecesidadRecurrente ? "RECURRENTE" : "EXTRAORDINARIA");
        if (necesidad instanceof NecesidadRecurrente recurrente) {
            dto.setPlazoEnDias(recurrente.getPlazoEnDias());
        }
        return dto;
    }

    private DonacionDTO convertirDonacionADTO(Donacion donacion) {
        DonacionDTO dto = new DonacionDTO();
        dto.setDonanteName(donacion.getDonante().darNombre());
        dto.setEntidadBeneficiaria(donacion.getEntidad() != null ? donacion.getEntidad().getRazonSocial() : null);
        dto.setDescripcion(donacion.getDescripcion());
        dto.setEstado(donacion.getEstado().name());
        dto.setSubcategoriaName(donacion.getSubcategoria().getNombre());
        dto.setCategoriaBienName(donacion.getSubcategoria().getCategoria().getNombre());
        dto.setFechaEntrega(donacion.getFechaEntrega());
        dto.setCantidadTotalBienes(donacion.sumaCantidadBienes());
        return dto;
    }

    public void agregarNecesidad(String razonSocial, NecesidadDTO dto) {
        EntidadBeneficiaria entidad = repositorio.buscarPorRazonSocial(razonSocial);
        SubcategoriaBien subcategoria = new SubcategoriaBien(dto.getNombreSubcategoria(), new CategoriaBien(dto.getNombreCategoria()));
        //Arreglar necesitamos repo de categorias

        Necesidad necesidad = switch (dto.getTipoNecesidad()) {
            case "RECURRENTE" -> new NecesidadRecurrente(
                    subcategoria,
                    dto.getDescripcion(),
                    dto.getCantidadObjetivo(),
                    dto.getPlazoEnDias()
            );
            case "EXTRAORDINARIA" -> new NecesidadExtraordinaria(
                    subcategoria,
                    dto.getDescripcion(),
                    dto.getCantidadObjetivo()
            );
            default -> throw new IllegalArgumentException("Tipo de necesidad inválido: " + dto.getTipoNecesidad());
        };

        entidad.agregarNecesidad(necesidad);
    }
}
