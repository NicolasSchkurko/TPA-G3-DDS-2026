package ar.edu.utn.frba.ddsi.donaciones.models.entities.SegmentadorDonaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.BienConEstado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.BienPerecedero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;


public class SegmentadorDonaciones {

    public SegmentadorDonaciones segmentador;

    public static List<Donacion> segmentar(Donante donante, List<Bien> bienesRecibidos) {
        Map<String, List<Bien>> grupos = new HashMap<>();

        for (Bien bien : bienesRecibidos) {
            String clave = generarClaveSegmentacion(bien);

            if (!grupos.containsKey(clave)) {
                grupos.put(clave, new ArrayList<>());
            }
            grupos.get(clave).add(bien);
        }

        List<Donacion> donacionesSegmentadas = new ArrayList<>();

        for (List<Bien> bienesDelGrupo : grupos.values()) {
            Donacion nuevaDonacion = crearDonacion(donante, bienesDelGrupo);
            donacionesSegmentadas.add(nuevaDonacion);
        }

        return donacionesSegmentadas;
    }

    private static String generarClaveSegmentacion(Bien bien) {
        // La unidad entra en la clave: sin esto, un grupo podía mezclar kilos con litros de la
        // misma subcategoría en una sola Donación, y Donacion.sumaCantidadBienes() los suma como
        // si fueran la misma unidad (15 = 10 kilos + 5 litros).
        String clave = bien.getSubcategoria().getNombre() + "-" + (bien.getUnidadUtilizada() != null ? bien.getUnidadUtilizada().name() : "SIN_UNIDAD");

        if (bien instanceof BienPerecedero perecedero) {
            clave = clave + "-" + perecedero.getFechaVencimiento().toString();
        }

        if (bien instanceof BienConEstado conEstado) {
            clave = clave + "-" + (conEstado.isUsado() ? "USADO" : "NUEVO");
        }
        return clave;
    }

    private static Donacion crearDonacion(Donante donante, List<Bien> bienesDelGrupo) {
        SubcategoriaBien sub = bienesDelGrupo.get(0).getSubcategoria();

        return new Donacion(
            donante,
            null,
            "Segmento de donación: " + sub.getNombre(),
            bienesDelGrupo,
            Estado.EN_DEPOSITO,
            sub,
            null
        );
    }
}
