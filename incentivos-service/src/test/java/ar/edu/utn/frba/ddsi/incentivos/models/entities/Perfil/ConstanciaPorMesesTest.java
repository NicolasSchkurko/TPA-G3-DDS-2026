package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.UnidadTiempo;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La racha tiene que contar MESES calendario consecutivos, no donaciones (punto 26).
 *
 * <p>La misión semilla "Racha" dice *"Realiza 1 donación durante 3 meses consecutivos"*
 * con {@code constancia = (1, MONTHS)} y {@code COINCIDENCIAS(3, "ENTREGADA")}. Antes la
 * única condición era que cada donación no tuviera más de un mes de antigüedad respecto de
 * la anterior, así que tres PATCH en tres días consecutivos completaban la misión de tres
 * meses.
 */
@DisplayName("Constancia: la racha se cuenta en meses, no en donaciones")
class ConstanciaPorMesesTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** La misión "Racha" del enunciado: 1 donación durante 3 meses consecutivos. */
    private static ProgresoMision rachaDeTresMeses() {
        ReglaConstancia constancia = new ReglaConstancia(1, UnidadTiempo.MONTHS);
        Regla regla = new Regla(
                constancia,
                AtributoImpacto.ESTADO,
                new CantidadCoincidencias(3, MAPPER.valueToTree("ENTREGADA"))
        );
        Mision mision = new Mision("Racha", null, "3 meses consecutivos",
                "Constancia solidaria", regla);
        return new ProgresoMision(mision);
    }

    private static ImpactoDonacion donacion(LocalDateTime fecha, String estado) {
        return new ImpactoDonacion(UUID.randomUUID(), UUID.randomUUID(),
                "Fundacion", 1, fecha, "ROPA", "INDUMENTARIA", estado);
    }

    private static List<ImpactoDonacion> rachasQueProgresan(ImpactoDonacion... donaciones) {
        List<ImpactoDonacion> lista = List.of(donaciones);
        lista.forEach(d -> d.registrarProgresoEn(null, true));
        return lista;
    }

    @Test
    @DisplayName("tres donaciones en tres días NO completan tres meses consecutivos")
    void tresDonacionesEnTresDiasNoCompletan() {
        ProgresoMision progreso = rachaDeTresMeses();

        LocalDateTime dia10 = LocalDateTime.of(2026, 3, 10, 10, 0);
        List<ImpactoDonacion> racha = rachasQueProgresan(
                donacion(dia10, "ENTREGADA"),
                donacion(dia10.plusDays(1), "ENTREGADA"),
                donacion(dia10.plusDays(2), "ENTREGADA")
        );

        progreso.evaluarConstancia(racha, dia10.plusDays(2));

        // Las tres caen en marzo, asi que la racha es de UN mes, no de tres.
        assertThat(progreso.getProgreso()).isEqualTo(1);
        assertThat(progreso.estaCompleta()).isFalse();
    }

    @Test
    @DisplayName("tres donaciones en tres meses distintos SÍ completan")
    void tresDonacionesEnTresMesesSiCompletan() {
        ProgresoMision progreso = rachaDeTresMeses();

        LocalDateTime marzo = LocalDateTime.of(2026, 3, 10, 10, 0);
        List<ImpactoDonacion> racha = rachasQueProgresan(
                donacion(marzo, "ENTREGADA"),
                donacion(marzo.plusMonths(1), "ENTREGADA"),
                donacion(marzo.plusMonths(2), "ENTREGADA")
        );

        progreso.evaluarConstancia(racha, marzo.plusMonths(2));

        assertThat(progreso.getProgreso()).isEqualTo(3);
        assertThat(progreso.estaCompleta()).isTrue();
    }

    @Test
    @DisplayName("dos donaciones en el mismo mes cuentan una sola vez")
    void dosDonacionesEnElMismoMesCuentanUna() {
        ProgresoMision progreso = rachaDeTresMeses();

        LocalDateTime marzo = LocalDateTime.of(2026, 3, 10, 10, 0);
        List<ImpactoDonacion> racha = rachasQueProgresan(
                donacion(marzo, "ENTREGADA"),
                donacion(marzo.plusDays(3), "ENTREGADA"),
                donacion(marzo.plusDays(20), "ENTREGADA"),
                donacion(marzo.plusMonths(1), "ENTREGADA")
        );

        progreso.evaluarConstancia(racha, marzo.plusMonths(1));

        // Marzo y abril: dos meses, aunque hubo cuatro donaciones.
        assertThat(progreso.getProgreso()).isEqualTo(2);
    }

    @Test
    @DisplayName("un mes sin donación corta la racha")
    void unMesSinDonacionCortaLaRacha() {
        ProgresoMision progreso = rachaDeTresMeses();

        LocalDateTime marzo = LocalDateTime.of(2026, 3, 10, 10, 0);
        List<ImpactoDonacion> racha = rachasQueProgresan(
                donacion(marzo, "ENTREGADA"),
                donacion(marzo.plusMonths(1), "ENTREGADA"),
                // Salta mayo.
                donacion(marzo.plusMonths(3), "ENTREGADA")
        );

        progreso.evaluarConstancia(racha, marzo.plusMonths(3));

        // Junio solo, porque mayo no tiene donación: la racha se corta en el hueco.
        assertThat(progreso.getProgreso()).isEqualTo(1);
    }

    @Test
    @DisplayName("la racha caduca si pasa el plazo desde la última donación")
    void laRachaCaducaSiPasaElPlazo() {
        ProgresoMision progreso = rachaDeTresMeses();

        LocalDateTime marzo = LocalDateTime.of(2026, 3, 10, 10, 0);
        List<ImpactoDonacion> racha = rachasQueProgresan(
                donacion(marzo, "ENTREGADA"),
                donacion(marzo.plusMonths(1), "ENTREGADA"),
                donacion(marzo.plusMonths(2), "ENTREGADA")
        );

        progreso.evaluarConstancia(racha, marzo.plusMonths(2));
        assertThat(progreso.getProgreso()).isEqualTo(3);

        // Con (1, MONTHS) hay que donar al menos una vez por mes: en septiembre venció.
        progreso.evaluarConstancia(racha, marzo.plusMonths(6));

        assertThat(progreso.getProgreso()).isZero();
    }

    @Test
    @DisplayName("las donaciones que no progresaron la misión no cuentan para la racha")
    void lasDonacionesQueNoProgresaronNoCuentan() {
        LocalDateTime marzo = LocalDateTime.of(2026, 3, 10, 10, 0);
        List<ImpactoDonacion> lista = List.of(
                donacion(marzo, "ENTREGADA"),
                donacion(marzo.plusMonths(1), "CANCELADA"),
                donacion(marzo.plusMonths(2), "ENTREGADA")
        );
        lista.get(0).registrarProgresoEn(null, true);
        lista.get(1).registrarProgresoEn(null, false);
        lista.get(2).registrarProgresoEn(null, true);

        ProgresoMision progreso = rachaDeTresMeses();
        progreso.evaluarConstancia(lista, marzo.plusMonths(2));

        // Marzo y mayo no son consecutivos: la racha es de uno solo.
        assertThat(progreso.getProgreso()).isEqualTo(1);
    }
}
