package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.UnidadTiempo;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Insignias: la misma no se otorga dos veces")
class InsigniaSinDuplicadosTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final UUID USUARIO = UUID.randomUUID();

    /** Misión con constancia de 1 mes, como la "Racha" del seed. */
    private static Mision racha() {
        ReglaConstancia constancia = new ReglaConstancia(1, UnidadTiempo.MONTHS);
        Regla regla = new Regla(
                constancia,
                AtributoImpacto.ESTADO,
                new CantidadCoincidencias(3, MAPPER.valueToTree("ENTREGADA"))
        );
        return new Mision("Racha", null, "3 meses consecutivos", "Constancia solidaria", regla);
    }

    private static ImpactoDonacion donacion(LocalDateTime fecha, UUID idDonacion) {
        ImpactoDonacion d = new ImpactoDonacion(
                idDonacion, USUARIO,
                "Fundacion", 1, fecha, "ROPA", "INDUMENTARIA", "ENTREGADA");
        d.registrarProgresoEn(null, true);
        return d;
    }

    /** Las tres donaciones con las que se completa la racha. */
    private static List<ImpactoDonacion> rachaCompleta() {
        LocalDateTime marzo = LocalDateTime.of(2026, 3, 10, 10, 0);
        return List.of(
                donacion(marzo, UUID.randomUUID()),
                donacion(marzo.plusMonths(1), UUID.randomUUID()),
                donacion(marzo.plusMonths(2), UUID.randomUUID())
        );
    }

    @Test
    @DisplayName("dos InsigniaObtenida del mismo perfil y misma insignia son iguales")
    void dosObtenidasDeLaMismaInsigniaSonIguales() {
        Perfil perfil = new Perfil(USUARIO, "Ana");
        Mision mision = racha();
        Insignia insignia = mision.getInsigniaObjetivo();

        InsigniaObtenida una = new InsigniaObtenida(perfil, insignia);
        InsigniaObtenida otra = new InsigniaObtenida(perfil, insignia);

        // Sin esto el Set de Perfil compararía por identidad y no deduparía nada.
        assertThat(una).isEqualTo(otra).hasSameHashCodeAs(otra);
    }

    @Test
    @DisplayName("el Set de insignias rechaza la insignia repetida")
    void elSetRechazaLaInsigniaRepetida() {
        Perfil perfil = new Perfil(USUARIO, "Ana");
        Insignia insignia = racha().getInsigniaObjetivo();

        assertThat(perfil.getInsigniasObtenidas().add(new InsigniaObtenida(perfil, insignia)))
                .isTrue();
        assertThat(perfil.getInsigniasObtenidas().add(new InsigniaObtenida(perfil, insignia)))
                .isFalse();

        assertThat(perfil.getInsigniasObtenidas()).hasSize(1);
    }

    @Test
    @DisplayName("insignias distintas sí se acumulan")
    void insigniasDistintasSiSeAcumulan() {
        Perfil perfil = new Perfil(USUARIO, "Ana");

        perfil.getInsigniasObtenidas()
              .add(new InsigniaObtenida(perfil, racha().getInsigniaObjetivo()));
        Perfil otro = new Perfil(UUID.randomUUID(), "Beto");
        Insignia otraInsignia = new Insignia("Otra", "desc");
        perfil.getInsigniasObtenidas()
              .add(new InsigniaObtenida(perfil, otraInsignia));

        assertThat(perfil.getInsigniasObtenidas()).hasSize(2);
    }

    @Test
    @DisplayName("re-completar la misma misión no guarda la insignia otra vez ni notifica")
    void reCompletarLaMismaMisionNoGuardaNiNotifica() {
        // La MISMA instancia de Mision en las dos categorias, como el seed.
        Mision racha = racha();
        Perfil perfil = new Perfil(USUARIO, "Ana");

        // Primer intento: completa y obtiene la insignia.
        perfil.cambiarMision(racha, null);
        List<ImpactoDonacion> historia = rachaCompleta();
        assertThat(perfil.progresarMision(historia.get(2), historia)).isTrue();
        assertThat(perfil.getInsigniasObtenidas()).hasSize(1);

        // Cambio de categoria: arranca de cero para la MISMA mision.
        perfil.cambiarMision(racha, racha);
        assertThat(perfil.getInsigniasObtenidas()).hasSize(1);

        // Segundo intento: el historial completo vuelve a completar la racha, pero la
        // insignia ya esta y no debe guardarse otra vez.
        List<ImpactoDonacion> historiaConNueva = new ArrayList<>(historia);
        historiaConNueva.add(donacion(LocalDateTime.of(2026, 6, 10, 10, 0), UUID.randomUUID()));

        boolean completo = perfil.progresarMision(
                historiaConNueva.get(3), historiaConNueva);

        // Sigue siendo "completó", para que el donante avance de misión y no quede
        // trabado en una que ya no puede completar.
        assertThat(completo).isTrue();
        assertThat(perfil.getInsigniasObtenidas()).hasSize(1);
    }
}
