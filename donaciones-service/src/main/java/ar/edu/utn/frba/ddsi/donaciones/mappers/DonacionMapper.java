package ar.edu.utn.frba.ddsi.donaciones.mappers;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadRecurrente;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class DonacionMapper {

    public DonacionDTO donaciontoDTO(Donacion donacion) {
        if (donacion == null) {
            return null;
        }

        DonacionDTO dto = new DonacionDTO();

        dto.setDonanteName(donacion.getDonante() != null ? donacion.getDonante().darNombre() : "Desconocido");
        dto.setEntidadBeneficiaria(donacion.getEntidad() != null ? donacion.getEntidad().getRazonSocial() : "No asignada");

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
        dto.setRazonSocial(entidad.getRazonSocial());
        dto.setTelefono(entidad.getNroTell() != null ? entidad.getNroTell().getNumeroDeTelefono() : null);
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
}
