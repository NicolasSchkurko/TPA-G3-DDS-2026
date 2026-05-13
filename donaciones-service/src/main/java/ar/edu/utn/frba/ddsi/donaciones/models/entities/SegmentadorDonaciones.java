package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

public class SegmentadorDonaciones {

    public List<Donacion> segmentar(PersonaDonante donante, List<Bien> bienesRecibidos) {
        // Agrupamos los bienes por una "clave de segmentación"
        Map<String, List<Bien>> grupos = bienesRecibidos.stream()
                .collect(Collectors.groupingBy(this::generarClaveSegmentacion));

        // Por cada grupo, creamos una Donación independiente
        return grupos.entrySet().stream()
                .map(entry -> crearDonacion(donante, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private String generarClaveSegmentacion(Bien bien) {
        // La clave identifica qué bienes PUEDEN ir juntos
        String clave = bien.getSubcategoria().getNombre();

        if (bien instanceof BienPerecedero) {
            clave += "-" + ((BienPerecedero) bien).getFechaVencimiento().toString();
        }

        if (bien instanceof BienConEstado) {
            clave += "-" + (((BienConEstado) bien).isUsado() ? "USADO" : "NUEVO");
        }

        return clave;
    }

    private Donacion crearDonacion(PersonaDonante donante, String clave, List<Bien> bienesDelGrupo) {
        SubcategoriaBien sub = bienesDelGrupo.get(0).getSubcategoria();

        return new Donacion(
                donante,
                null,
                "Segmento de donación: " + sub.getNombre(),
                bienesDelGrupo,
                Estados.EN_DEPOSITO,
                sub
        );
    }
}
