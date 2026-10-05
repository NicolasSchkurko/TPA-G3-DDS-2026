package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProgresoMision: avance y evaluacion de constancia")
class ProgresoMisionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Mision mision(Regla regla, String nombreInsignia) {
        return new Mision("Mision de prueba", null, "Descripcion", nombreInsignia, regla);
    }

    private static ImpactoDonacion donacion(LocalDateTime fecha, Integer bienes, String estado) {
        return new ImpactoDonacion(
                "Fundacion", bienes, fecha, "ALIMENTOS", "MERCEARIA", estado, UUID.randomUUID());
    }

    @Test
    @DisplayName("arranca en cero y no completa la insignia hasta alcanzar el objetivo")
    void avanzaHastaCompletarLaMision() {
        Regla regla = new Regla(null, AtributoImpacto.CANTIDAD_BIENES, new SuperaCantidad(2, 5));
        ProgresoMision progreso = new ProgresoMision(mision(regla, "Habil"));

        assertThat(progreso.getProgreso()).isZero();

        assertThat(progreso.progresarMision(
                donacion(LocalDateTime.of(2026, 3, 1, 10, 0), 7, "ENTREGADA"), List.of()))
                .isNull();
        assertThat(progreso.getProgreso()).isEqualTo(1);

        Insignia insignia = progreso.progresarMision(
                donacion(LocalDateTime.of(2026, 3, 2, 10, 0), 6, "ENTREGADA"), List.of());

        assertThat(insignia).isNotNull();
        assertThat(insignia.getNombre()).isEqualTo("Habil");
    }

    @Test
    @DisplayName("marca en la donacion si hizo progresar la mision actual")
    void marcaLaDonacionQueProgresa() {
        Regla regla = new Regla(
                null,
                AtributoImpacto.ESTADO,
                new CantidadCoincidencias(5, MAPPER.valueToTree("ENTREGADA"))
        );
        ProgresoMision progreso = new ProgresoMision(mision(regla, "Entregas"));

        ImpactoDonacion queProgresa = donacion(LocalDateTime.of(2026, 3, 1, 10, 0), 1, "ENTREGADA");
        progreso.progresarMision(queProgresa, List.of());
        assertThat(queProgresa.getHizoProgresarMision()).isTrue();
        assertThat(progreso.getProgreso()).isEqualTo(1);

        ImpactoDonacion noApta = donacion(LocalDateTime.of(2026, 3, 2, 10, 0), 1, "CANCELADA");
        progreso.progresarMision(noApta, List.of());
        assertThat(noApta.getHizoProgresarMision()).isFalse();
        assertThat(progreso.getProgreso()).isEqualTo(1);
    }

    @Test
    @DisplayName("la constancia cuenta racha y se reinicia al vencer el plazo")
    void laConstanciaSeReiniciaAlVencerseElPlazo() {
        ReglaConstancia constancia = new ReglaConstancia(1, ChronoUnit.MONTHS);
        Regla regla = new Regla(
                constancia,
                AtributoImpacto.ESTADO,
                new CantidadCoincidencias(3, MAPPER.valueToTree("ENTREGADA"))
        );
        ProgresoMision progreso = new ProgresoMision(mision(regla, "Constante"));

        LocalDateTime marzo = LocalDateTime.of(2026, 3, 10, 10, 0);
        List<ImpactoDonacion> racha = List.of(
                donacion(marzo, 1, "ENTREGADA"),
                donacion(marzo.plusMonths(1), 1, "ENTREGADA"),
                donacion(marzo.plusMonths(2), 1, "ENTREGADA")
        );
        racha.forEach(d -> d.setHizoProgresarMision(true));

        progreso.evaluarConstancia(racha, marzo.plusMonths(2));
        assertThat(progreso.getProgreso()).isEqualTo(3);

        // Pasaron mas de 3 meses desde la ultima donacion: la racha caduca.
        progreso.evaluarConstancia(racha, marzo.plusMonths(6));
        assertThat(progreso.getProgreso()).isZero();
    }

    @Test
    @DisplayName("la constancia ignora las donaciones que no hicieron progresar la mision")
    void laConstanciaIgnoraDonacionesQueNoProgressaron() {
        ReglaConstancia constancia = new ReglaConstancia(1, ChronoUnit.MONTHS);
        Regla regla = new Regla(
                constancia,
                AtributoImpacto.ESTADO,
                new CantidadCoincidencias(2, MAPPER.valueToTree("ENTREGADA"))
        );
        ProgresoMision progreso = new ProgresoMision(mision(regla, "Constante"));

        LocalDateTime marzo = LocalDateTime.of(2026, 3, 10, 10, 0);
        List<ImpactoDonacion> donations = List.of(
                donacion(marzo, 1, "ENTREGADA"),
                donacion(marzo.plusMonths(1), 1, "ENTREGADA"),
                donacion(marzo.plusMonths(2), 1, "CANCELADA")
        );
        donations.getFirst().setHizoProgresarMision(true);
        donations.get(1).setHizoProgresarMision(true);
        donations.get(2).setHizoProgresarMision(false);

        progreso.evaluarConstancia(donations, marzo.plusMonths(2));

        assertThat(progreso.getProgreso()).isEqualTo(2);
    }

    @Test
    @DisplayName("sin donaciones la constancia deja el progreso en cero")
    void sinDonacionesElProgresoEsCero() {
        ReglaConstancia constancia = new ReglaConstancia(1, ChronoUnit.MONTHS);
        Regla regla = new Regla(constancia, AtributoImpacto.ESTADO,
                new CantidadCoincidencias(1, MAPPER.valueToTree("ENTREGADA")));
        ProgresoMision progreso = new ProgresoMision(mision(regla, "Constante"));

        progreso.evaluarConstancia(List.of(), LocalDateTime.now());

        assertThat(progreso.getProgreso()).isZero();
    }

    @Test
    @DisplayName("estaCompleta consulta a la operacion de la regla")
    void estaCompletaConsultaALaOperacion() {
        Regla regla = new Regla(null, AtributoImpacto.CANTIDAD_BIENES, new SuperaCantidad(2, 1));
        ProgresoMision progreso = new ProgresoMision(mision(regla, "Habil"));
        progreso.setProgreso(2);

        assertThat(progreso.estaCompleta()).isTrue();

        progreso.setProgreso(1);
        assertThat(progreso.estaCompleta()).isFalse();
    }
}