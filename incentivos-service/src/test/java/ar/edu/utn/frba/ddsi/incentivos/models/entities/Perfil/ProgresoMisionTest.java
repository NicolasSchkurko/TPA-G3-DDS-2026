package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.ValoresDistintos;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ProgresoMision: avance y evaluacion de constancia")
class ProgresoMisionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Mision mision(Regla regla, String nombreInsignia) {
        return new Mision("Mision de prueba", null, "Descripcion", nombreInsignia, regla);
    }

    private static ImpactoDonacion donacion(LocalDateTime fecha, Integer bienes, String estado) {
        return new ImpactoDonacion(UUID.randomUUID(), UUID.randomUUID(),
                "Fundacion", bienes, fecha, "ALIMENTOS", "MERCEARIA", estado);
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
        racha.forEach(d -> d.registrarProgresoEn(null, true));

        progreso.evaluarConstancia(racha, marzo.plusMonths(2));
        assertThat(progreso.getProgreso()).isEqualTo(3);

        // Pasaron mas de 3 meses desde la ultima donacion: la racha caduca.
        progreso.evaluarConstancia(racha, marzo.plusMonths(6));
        assertThat(progreso.getProgreso()).isZero();
    }

    @Test
    @DisplayName("la constancia ignora las donaciones que no hicieron progresar la mision")
    void laConstanciaIgnoraDonacionesQueNoProgresaron() {
        ReglaConstancia constancia = new ReglaConstancia(1, ChronoUnit.MONTHS);
        Regla regla = new Regla(
                constancia,
                AtributoImpacto.ESTADO,
                new CantidadCoincidencias(2, MAPPER.valueToTree("ENTREGADA"))
        );
        final ProgresoMision progreso = new ProgresoMision(mision(regla, "Constante"));

        LocalDateTime marzo = LocalDateTime.of(2026, 3, 10, 10, 0);
        List<ImpactoDonacion> donaciones = List.of(
                donacion(marzo, 1, "ENTREGADA"),
                donacion(marzo.plusMonths(1), 1, "ENTREGADA"),
                donacion(marzo.plusMonths(2), 1, "CANCELADA")
        );
        donaciones.getFirst().registrarProgresoEn(null, true);
        donaciones.get(1).registrarProgresoEn(null, true);
        donaciones.get(2).registrarProgresoEn(null, false);

        progreso.evaluarConstancia(donaciones, marzo.plusMonths(2));

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
        // La regla pide 2 donaciones de al menos 1 bien, así que se avanza con donaciones
        // reales en vez de con un setter.
        Regla regla = new Regla(null, AtributoImpacto.CANTIDAD_BIENES, new SuperaCantidad(2, 1));
        ProgresoMision progreso = new ProgresoMision(mision(regla, "Habil"));

        progreso.progresarMision(donacion(LocalDateTime.of(2026, 3, 1, 10, 0), 3, "ENTREGADA"), List.of());
        assertThat(progreso.estaCompleta()).isFalse();

        progreso.progresarMision(donacion(LocalDateTime.of(2026, 3, 2, 10, 0), 3, "ENTREGADA"), List.of());
        assertThat(progreso.estaCompleta()).isTrue();
    }

    @Nested
    @DisplayName("Valores distintos: el avance es del donante, no de la mision")
    class ValoresDistintosPorDonante {

        /** "6 donaciones de 3 categorias distintas", mirando CATEGORIA. */
        private Mision misionDeCategorias() {
            Regla regla = new Regla(
                    null,
                    AtributoImpacto.CATEGORIA,
                    new ValoresDistintos(2, 3)
            );
            return mision(regla, "Completitud");
        }

        private ImpactoDonacion donacionDeCategoria(LocalDateTime fecha, String categoria) {
            return new ImpactoDonacion(UUID.randomUUID(), UUID.randomUUID(),
                    "Fundacion", 1, fecha, categoria, "MERCEARIA", "ENTREGADA"
            );
        }

        @Test
        @DisplayName("las categorias que vio uno no completan la mision de otro")
        void lasCategoriasDeUnoNoCompletanLaMisionDeOtro() {
            Mision mision = misionDeCategorias();

            // Ana y Beto hacen la MISMA mision, o sea comparten la entidad ValoresDistintos.
            ProgresoMision ana = new ProgresoMision(mision);
            ProgresoMision beto = new ProgresoMision(mision);

            LocalDateTime marzo = LocalDateTime.of(2026, 3, 1, 10, 0);
            ana.progresarMision(donacionDeCategoria(marzo, "INDUMENTARIA"), List.of());
            beto.progresarMision(donacionDeCategoria(marzo, "ALIMENTOS"), List.of());
            beto.progresarMision(donacionDeCategoria(marzo.plusDays(1), "MUEBLES"), List.of());

            assertThat(beto.cantidadValoresObservados()).isEqualTo(2);
            assertThat(ana.cantidadValoresObservados()).isEqualTo(1);

            // Beto va 2 de 2 donaciones, pero solo vio 2 de las 3 categorias: no completa.
            assertThat(beto.estaCompleta()).isFalse();
            // Ana solo hizo una donacion, y no le sirven las categorias de Beto.
            assertThat(ana.estaCompleta()).isFalse();
            assertThat(ana.getProgreso()).isEqualTo(1);
        }

        @Test
        @DisplayName("la insignia se otorga solo a quien completo su propia cuenta")
        void laInsigniaSeOtorgaSoloAQuienLaCompleto() {
            Mision mision = misionDeCategorias();

            ProgresoMision ana = new ProgresoMision(mision);
            ProgresoMision beto = new ProgresoMision(mision);

            LocalDateTime marzo = LocalDateTime.of(2026, 3, 1, 10, 0);
            ana.progresarMision(donacionDeCategoria(marzo, "INDUMENTARIA"), List.of());
            beto.progresarMision(donacionDeCategoria(marzo, "ALIMENTOS"), List.of());
            beto.progresarMision(donacionDeCategoria(marzo.plusDays(1), "MUEBLES"), List.of());

            // Carla dona 3 veces con 3 categorias distintas: cumple las dos condiciones
            // (2 donaciones y 3 categorias).
            ProgresoMision carla = new ProgresoMision(mision);
            carla.progresarMision(donacionDeCategoria(marzo, "SALUD"), List.of());
            carla.progresarMision(donacionDeCategoria(marzo.plusDays(1), "LIBROS"), List.of());
            Insignia deCarla = carla.progresarMision(
                    donacionDeCategoria(marzo.plusDays(2), "JUGUETES"), List.of());
            assertThat(deCarla).isNotNull();
            assertThat(carla.estaCompleta()).isTrue();

            // Ana y Beto no se han ganado nada, aunque las categorias "existan"
            // en la mision porque otro las dono.
            assertThat(ana.estaCompleta()).isFalse();
            assertThat(beto.estaCompleta()).isFalse();
        }

        @Test
        @DisplayName("una donacion sin categoria no suma valor distinto")
        void unaDonacionSinCategoriaNoSumaValorDistinto() {
            ProgresoMision progreso = new ProgresoMision(misionDeCategorias());

            ImpactoDonacion sinCategoria = new ImpactoDonacion(
                    UUID.randomUUID(), UUID.randomUUID(),
                    "Fundacion", 1, LocalDateTime.of(2026, 3, 1, 10, 0),
                    null, null, "ENTREGADA"
            );
            progreso.progresarMision(sinCategoria, List.of());

            assertThat(sinCategoria.getHizoProgresarMision()).isFalse();
            assertThat(progreso.cantidadValoresObservados()).isZero();
            assertThat(progreso.getProgreso()).isZero();
        }

        @Test
        @DisplayName("al romperse la constancia se descartan los valores de la racha vieja")
        void alRomperseLaConstanciaSeDescartanLosValoresViejos() {
            ReglaConstancia constancia = new ReglaConstancia(1, ChronoUnit.MONTHS);
            Regla regla = new Regla(
                    constancia,
                    AtributoImpacto.CATEGORIA,
                    new ValoresDistintos(2, 2)
            );
            ProgresoMision progreso = new ProgresoMision(mision(regla, "Constante"));

            LocalDateTime marzo = LocalDateTime.of(2026, 3, 10, 10, 0);
            ImpactoDonacion primera = donacionDeCategoria(marzo, "ALIMENTOS");
            progreso.progresarMision(primera, List.of());

            // Como lo hace el service, la segunda evaluacion recibe el historial.
            progreso.progresarMision(
                    donacionDeCategoria(marzo.plusMonths(1), "MUEBLES"), List.of(primera));

            assertThat(progreso.getProgreso()).isEqualTo(2);
            assertThat(progreso.cantidadValoresObservados()).isEqualTo(2);

            // La racha vencio: el donante arranca de cero y las categorias que vio antes
            // del corte tampoco cuentan.
            progreso.evaluarConstancia(List.of(), LocalDateTime.of(2026, 12, 1, 10, 0));

            assertThat(progreso.getProgreso()).isZero();
            assertThat(progreso.cantidadValoresObservados()).isZero();
        }

        @Test
        @DisplayName("cambiar de mision deja los valores atras")
        void cambiarDeMisionDejaLosValoresAtras() {
            ProgresoMision ana = new ProgresoMision(misionDeCategorias());
            ana.progresarMision(
                    donacionDeCategoria(LocalDateTime.of(2026, 3, 1, 10, 0), "ALIMENTOS"),
                    List.of());

            ProgresoMision nuevo = new ProgresoMision(misionDeCategorias());

            assertThat(nuevo.cantidadValoresObservados()).isZero();
            assertThat(ana.cantidadValoresObservados()).isEqualTo(1);
        }
    }
}
