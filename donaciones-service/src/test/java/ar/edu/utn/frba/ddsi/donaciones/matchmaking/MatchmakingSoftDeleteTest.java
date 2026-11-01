package ar.edu.utn.frba.ddsi.donaciones.matchmaking;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AlgoritmosDeAsignacion.CompatibilidadSemantica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AlgoritmosDeAsignacion.SubAtendidos;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.BienPerecedero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadExtraordinaria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MatchmakingSoftDeleteTest {

    @Test
    @DisplayName("Punto 70: CompatibilidadSemantica y SubAtendidos ignoran entidades inactivas")
    void matchmaking_ignoraEntidadesInactivas() {
        CategoriaBien alimentos = new CategoriaBien("Alimentos");
        SubcategoriaBien leche = new SubcategoriaBien("Leche", alimentos);

        // Donacion compatible
        Donacion donacion = new Donacion();
        donacion.setDescripcion("Cajas de leche");
        donacion.setSubcategoria(leche);
        BienPerecedero bien = new BienPerecedero("Leche", leche, null, 10, UnidadDeMedida.UNIDADES, LocalDate.now().plusMonths(1));
        donacion.agregarBien(bien);

        // Entidad inactiva con necesidad compatible
        EntidadBeneficiaria entidadInactiva = new EntidadBeneficiaria();
        Necesidad necesidad = new NecesidadExtraordinaria(leche, "Leche", 50);
        entidadInactiva.agregarNecesidad(necesidad);
        entidadInactiva.darDeBaja(); // marca entidad y necesidad como inactivas

        CompatibilidadSemantica compatibilidad = new CompatibilidadSemantica();
        List<PropuestaAsignacion> propuestasComp = compatibilidad.rankear(donacion, List.of(entidadInactiva));
        assertTrue(propuestasComp.isEmpty(), "CompatibilidadSemantica no debe generar propuestas para entidades inactivas");

        SubAtendidos subAtendidos = new SubAtendidos();
        List<PropuestaAsignacion> propuestasSub = subAtendidos.rankear(donacion, List.of(entidadInactiva));
        assertTrue(propuestasSub.isEmpty(), "SubAtendidos no debe generar propuestas para entidades inactivas");
    }

    @Test
    @DisplayName("Punto 70: Matchmaking ignora necesidades inactivas en entidades activas")
    void matchmaking_ignoraNecesidadesInactivas() {
        CategoriaBien alimentos = new CategoriaBien("Alimentos");
        SubcategoriaBien leche = new SubcategoriaBien("Leche", alimentos);

        Donacion donacion = new Donacion();
        donacion.setDescripcion("Cajas de leche");
        donacion.setSubcategoria(leche);
        BienPerecedero bien = new BienPerecedero("Leche", leche, null, 10, UnidadDeMedida.UNIDADES, LocalDate.now().plusMonths(1));
        donacion.agregarBien(bien);

        EntidadBeneficiaria entidadActiva = new EntidadBeneficiaria();
        Necesidad necesidadCancelada = new NecesidadExtraordinaria(leche, "Leche", 50);
        necesidadCancelada.setActiva(false); // cancelada lógicamente
        entidadActiva.agregarNecesidad(necesidadCancelada);

        CompatibilidadSemantica compatibilidad = new CompatibilidadSemantica();
        List<PropuestaAsignacion> propuestas = compatibilidad.rankear(donacion, List.of(entidadActiva));
        assertTrue(propuestas.isEmpty(), "No debe emparejar con necesidades canceladas lógicamente");
    }
}
