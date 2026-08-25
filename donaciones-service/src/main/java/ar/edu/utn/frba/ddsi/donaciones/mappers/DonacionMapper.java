package ar.edu.utn.frba.ddsi.donaciones.mappers;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.BienResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.*;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadRecurrente;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class DonacionMapper {

    public DonacionDTO donacionToDTO(Donacion donacion) {
        if (donacion == null) {
            return null;
        }

        DonacionDTO dto = new DonacionDTO();

        dto.setDonanteName(donacion.getDonante() != null ? donacion.getDonante().getPersona().getNombreDeUsuario() : "Desconocido");
        dto.setEntidadBeneficiaria(donacion.getEntidad() != null ? donacion.getEntidad().getPersonaJuridica().getRazonSocial() : "No asignada");

        dto.setDescripcion(donacion.getDescripcion());
        dto.setEstado(donacion.getEstado() != null ? donacion.getEstado().name() : "N/A");

        dto.setSubcategoriaName(donacion.getSubcategoria() != null ? donacion.getSubcategoria().getNombre() : "N/A");
        dto.setCategoriaBienName("Categoría Pendiente");

        dto.setFechaEntrega(donacion.getFechaEntrega());
        dto.setCantidadTotalBienes(donacion.sumaCantidadBienes());
        dto.setBienes(new ArrayList<>());

        return dto;
    }

    public EntidadBeneficiariaDTO entidadtoDTO(EntidadBeneficiaria entidad) {
        EntidadBeneficiariaDTO dto = new EntidadBeneficiariaDTO();
        // Si a futuro añades id a EntidadBeneficiariaDTO, deberías setearlo aquí (dto.setId(entidad.getId());)
        dto.setRazonSocial(entidad.getPersonaJuridica().getRazonSocial());
        dto.setTelefono(
                entidad.getPersonaJuridica().getMediosDeContacto().getMedioDeContactoPredeterminado().getValor() != null ?
                        entidad.getPersonaJuridica().getMediosDeContacto().getMedioDeContactoPredeterminado().getValor() : null);
        return dto;
    }

    public NecesidadDTO necesidadtoDTO(Necesidad necesidad) {
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

    public Donacion dtoToDonacion(DonacionDTO donacionDTO) {
        Donacion donacion = new Donacion();

        donacion.setDescripcion(donacionDTO.getDescripcion());
        donacion.setBienes(donacionDTO.getBienes().stream().map(this::dtoToBien).toList());

        return donacion;
    }

    private Bien dtoToBien(BienResumenDTO bienDTO) {

        String descripcion = bienDTO.getDescripcion();
        CategoriaBien categoriaBien = new CategoriaBien(bienDTO.getCategoria());
        SubcategoriaBien subcategoriaBien = new SubcategoriaBien(bienDTO.getSubcategoria(), categoriaBien);
        Integer cantidad = bienDTO.getCantidad();
        String foto = bienDTO.getUrlFoto();
        UnidadDeMedida unidadDeMedida = switch (bienDTO.getUnidadDeMedida().toLowerCase()) {
            case "kilogramos", "kilos", "kg" -> UnidadDeMedida.KILOGRAMOS;
            case "litros", "lt" -> UnidadDeMedida.LITROS;
            default -> null;
        };

        return switch (bienDTO.getTipoBien().toLowerCase()) {
            case "con estado", "conestado" -> new BienConEstado(
                    descripcion,
                    subcategoriaBien,
                    foto,
                    cantidad,
                    unidadDeMedida,
                    bienDTO.getUsado()
            );
            case "perecedero" -> new BienPerecedero(
                    descripcion,
                    subcategoriaBien,
                    foto,
                    cantidad,
                    unidadDeMedida,
                    bienDTO.getFechaVencimiento()
            );
            default -> null;
        };
    }
}
