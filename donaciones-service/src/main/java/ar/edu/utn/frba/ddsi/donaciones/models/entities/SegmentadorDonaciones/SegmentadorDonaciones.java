package ar.edu.utn.frba.ddsi.donaciones.models.entities.SegmentadorDonaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.BienConEstado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.BienPerecedero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;

import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;


public class SegmentadorDonaciones {

    public SegmentadorDonaciones segmentador;

    /** La fecha de realización del formulario queda como fechaEntrega de cada donación
     *  (punto 10 de PENDIENTES.md): es la que usan incentivos y los conteos por período. */
    public static List<Donacion> segmentar(Donante donante, List<Bien> bienesRecibidos, LocalDate fechaEntrega) {
        Map<String, List<Bien>> grupos = new HashMap<>();

        // Agrupa los bienes bajo la misma clave de segmentación.
        for (Bien bien : bienesRecibidos) {
            grupos.computeIfAbsent(generarClaveSegmentacion(bien), k -> new ArrayList<>()).add(bien);
        }

        List<Donacion> donacionesSegmentadas = new ArrayList<>();

        for (List<Bien> bienesDelGrupo : grupos.values()) {
            Donacion nuevaDonacion = crearDonacion(donante, bienesDelGrupo, fechaEntrega);
            donacionesSegmentadas.add(nuevaDonacion);
        }

        return donacionesSegmentadas;
    }

    private static String generarClaveSegmentacion(Bien bien) {
        // La unidad entra en la key: dos bienes de la misma subcategoría con distinta unidad no
        // pueden sumarse (kilos no son litros), y logística guarda UN par cantidad+unidad por
        // donación (puntos 23 y 31 de PENDIENTES.md: cada segmento queda de una sola unidad).
        String clave = bien.getSubcategoria().getNombre();

        if (bien.getUnidadUtilizada() != null) {
            clave = clave + "-" + bien.getUnidadUtilizada().name();
        }

        if (bien instanceof BienPerecedero perecedero) {
            clave = clave + "-" + perecedero.getFechaVencimiento().toString();
        }

        if (bien instanceof BienConEstado conEstado) {
            clave = clave + "-" + (conEstado.isUsado() ? "USADO" : "NUEVO");
        }
        return clave;
    }

    private static Donacion crearDonacion(Donante donante, List<Bien> bienesDelGrupo, LocalDate fechaEntrega) {
        SubcategoriaBien sub = bienesDelGrupo.get(0).getSubcategoria();

        return new Donacion(
            donante,
            null,
            "Segmento de donación: " + sub.getNombre(),
            bienesDelGrupo,
            Estado.EN_DEPOSITO,
            sub,
            fechaEntrega
        );
    }
}
