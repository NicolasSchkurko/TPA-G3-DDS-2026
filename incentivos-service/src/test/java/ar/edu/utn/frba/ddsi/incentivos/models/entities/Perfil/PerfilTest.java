package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.OperacionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.UnidadTiempo;
import ar.edu.utn.frba.ddsi.incentivos.models.events.CategoriaNuevaPublicar;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCompletada;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Perfil")
class PerfilTest {
        @Nested
        @DisplayName("Perfil: raiz de agregado")
        class Agregado {


        private static final UUID ID_USUARIO = UUID.randomUUID();

        private static Mision mision(String nombre) {
            Regla regla = new Regla(null, AtributoImpacto.CANTIDAD_BIENES, new SuperaCantidad(1, 5));
            return new Mision(nombre, null, "Descripcion", "Insignia " + nombre, regla);
        }

        private static ImpactoDonacion donacion() {
            return new ImpactoDonacion(UUID.randomUUID(), ID_USUARIO, "Fundacion", 7,
                    LocalDateTime.of(2026, 3, 1, 10, 0),
                    "ALIMENTOS", "MERCEARIA", "ENTREGADA");
        }

        private static Categoria categoria(String nombre, Mision... misiones) {
            return new Categoria(nombre, null, 1, new ArrayList<>(List.of(misiones)));
        }

        /** {@code domainEvents()} es protected: se accede por reflexión para no exponer API de test. */
        @SuppressWarnings("unchecked")
        private static <T> List<T> eventos(Perfil perfil) {
            Collection<Object> eventos = ReflectionTestUtils.invokeMethod(perfil, "domainEvents");
            return List.copyOf((Collection<T>) eventos);
        }

        @Test
        @DisplayName("nace sin insignias, sin categoria y sin mision")
        void naceEnEstadoInicial() {
            Perfil perfil = new Perfil(ID_USUARIO, "Ana");

            assertThat(perfil.getIdUsuario()).isEqualTo(ID_USUARIO);
            assertThat(perfil.getNombreUsuario()).isEqualTo("Ana");
            assertThat(perfil.getCategoriaActual()).isNull();
            assertThat(perfil.getProgresoMisionActual()).isNull();
            assertThat(perfil.getInsigniasObtenidas()).isEmpty();
        }

        @Test
        @DisplayName("sin mision asignada no progresa")
        void sinMisionNoProgresa() {
            Perfil perfil = new Perfil(ID_USUARIO, "Ana");

            assertThat(perfil.progresarMision(donacion(), List.of())).isFalse();
        }

        @Test
        @DisplayName("al completar la mision otorga la insignia y registra el evento")
        void alCompletarOtorgaInsigniaYRegistraEvento() {
            Perfil perfil = new Perfil(ID_USUARIO, "Ana");
            perfil.cambiarMision(mision("Primera"), null);

            assertThat(perfil.progresarMision(donacion(), List.of())).isTrue();

            assertThat(perfil.getInsigniasObtenidas()).hasSize(1);
            assertThat(perfil.getInsigniasObtenidas().iterator().next().getInsignia().getNombre())
                    .isEqualTo("Insignia Primera");

            List<MisionCompletada> eventos = eventos(perfil);
            assertThat(eventos).hasSize(1);
            assertThat(eventos.getFirst().misionAnterior()).isEqualTo("Primera");
            assertThat(eventos.getFirst().insigniaObtenida()).isEqualTo("Insignia Primera");
            assertThat(eventos.getFirst().idUsuario()).isEqualTo(ID_USUARIO);
        }

        @Test
        @DisplayName("cambiar de mision reemplaza el progreso y registra el evento")
        void cambiarDeMisionRegistraElEvento() {
            Perfil perfil = new Perfil(ID_USUARIO, "Ana");
            Mision anterior = mision("Primera");
            Mision nueva = mision("Racha");
            perfil.cambiarMision(anterior, null);

            perfil.cambiarMision(nueva, anterior);

            assertThat(perfil.getProgresoMisionActual().getMision()).isSameAs(nueva);
            assertThat(perfil.getProgresoMisionActual().getProgreso()).isZero();

            List<MisionCambiada> eventos = eventos(perfil);
            assertThat(eventos).hasSize(1);
            assertThat(eventos.getFirst().misionAnterior()).isEqualTo("Primera");
            assertThat(eventos.getFirst().misionNueva()).isEqualTo("Racha");
            assertThat(eventos.getFirst().insigniaAnterior()).isEqualTo("Insignia Primera");
        }

        @Test
        @DisplayName("cambiar a una categoria sin misiones deja el progreso vacio")
        void cambiarACategoriaSinMisionesDejaProgresoVacio() {
            Perfil perfil = new Perfil(ID_USUARIO, "Ana");
            Categoria origen = categoria("Colaborador", mision("Primera"));
            // iniciarEn ya deja al donante en la primera misión de "Colaborador".
            perfil.iniciarEn(origen);

            perfil.cambiarCategoria(categoria("Vacia"), origen, mision("Primera"));

            assertThat(perfil.getCategoriaActual().getNombre()).isEqualTo("Vacia");
            assertThat(perfil.getProgresoMisionActual()).isNull();
        }

        @Test
        @DisplayName("al cambiar de categoria arranca en la primera mision y registra ambos eventos")
        void cambiarDeCategoriaRegistraCategoriaYMision() {
            Perfil perfil = new Perfil(ID_USUARIO, "Ana");
            Categoria origen = categoria("Colaborador", mision("Primera"));
            Categoria destino = categoria("Sostenedor", mision("Racha"));
            perfil.iniciarEn(origen);

            perfil.cambiarCategoria(destino, origen, mision("Primera"));

            assertThat(perfil.getProgresoMisionActual().getMision().getNombreMision())
                    .isEqualTo("Racha");

            assertThat(eventos(perfil))
                    .hasSize(2)
                    .anyMatch(e -> e instanceof CategoriaNuevaPublicar)
                    .anyMatch(e -> e instanceof MisionCambiada);

            CategoriaNuevaPublicar cambio = eventos(perfil).stream()
                    .filter(e -> e instanceof CategoriaNuevaPublicar)
                    .map(e -> (CategoriaNuevaPublicar) e)
                    .findFirst()
                    .orElseThrow();

            assertThat(cambio.categoriaAnterior()).isEqualTo("Colaborador");
            assertThat(cambio.categoriaNueva()).isEqualTo("Sostenedor");
        }

        @Test
        @DisplayName("la insignia obtenida queda asociada al perfil")
        void laInsigniaQuedaAsociadaAlPerfil() {
            Perfil perfil = new Perfil(ID_USUARIO, "Ana");
            Insignia insignia = new Insignia("Insignia", "Descripcion");
            perfil.cambiarMision(mision("Primera"), null);

            perfil.progresarMision(donacion(), List.of());

            InsigniaObtenida obtenida = perfil.getInsigniasObtenidas().iterator().next();
            assertThat(obtenida.getPerfil()).isSameAs(perfil);
            assertThat(obtenida.getFechaObtencion()).isNotNull();
            assertThat(obtenida.getInsignia()).isNotNull();
        }
        @Test
        @DisplayName("darDeBaja desactiva el perfil, anonimiza nombre y limpia insignias y misiones")
        void darDeBajaDesactivaYMinimizaDatos() {
            Perfil perfil = new Perfil(ID_USUARIO, "Ana");
            Categoria cat = categoria("Base", mision("M1"));
            perfil.iniciarEn(cat);

            Insignia insignia = new Insignia("Medalla", "Desc");
            perfil.getInsigniasObtenidas().add(new InsigniaObtenida(perfil, insignia));

            assertThat(perfil.isActivo()).isTrue();
            assertThat(perfil.getNombreUsuario()).isEqualTo("Ana");
            assertThat(perfil.getCategoriaActual()).isNotNull();
            assertThat(perfil.getProgresoMisionActual()).isNotNull();
            assertThat(perfil.getInsigniasObtenidas()).isNotEmpty();

            perfil.darDeBaja();

            assertThat(perfil.isActivo()).isFalse();
            assertThat(perfil.getNombreUsuario()).isNull();
            assertThat(perfil.getCategoriaActual()).isNull();
            assertThat(perfil.getProgresoMisionActual()).isNull();
            assertThat(perfil.getInsigniasObtenidas()).isEmpty();
        }
        }

        @Nested
        @DisplayName("Insignias: la misma no se otorga dos veces")
        class InsigniasSinDuplicados {


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
        @DisplayName("dos instancias de la misma insignia son iguales por id")
        void dosInstanciasDeLaMismaInsigniaSonIguales() throws Exception {
            UUID id = UUID.randomUUID();
            Insignia una = insigniaConId(id, "Medalla");
            Insignia otra = insigniaConId(id, "Medalla");

            assertThat(una).isEqualTo(otra).hasSameHashCodeAs(otra);
        }

        @Test
        @DisplayName("el Set deduplica aunque el perfil sea otra instancia con el mismo id")
        void elSetDeduplicaAunqueElPerfilSeaOtraInstancia() {
            UUID id = UUID.randomUUID();
            Perfil uno = new Perfil(id, "Ana");
            Perfil otro = new Perfil(id, "Ana");
            Insignia insignia = new Insignia("Medalla", "desc");

            InsigniaObtenida una = new InsigniaObtenida(uno, insignia);
            InsigniaObtenida otra = new InsigniaObtenida(otro, insignia);

            assertThat(una).isEqualTo(otra).hasSameHashCodeAs(otra);
        }

        @Test
        @DisplayName("el Set rechaza la misma insignia aunque venga como otra instancia")
        void elSetRechazaLaMismaInsigniaEnOtraInstancia() throws Exception {
            UUID id = UUID.randomUUID();
            Perfil perfil = new Perfil(USUARIO, "Ana");
            Insignia una = insigniaConId(id, "Medalla");
            Insignia otra = insigniaConId(id, "Medalla");

            assertThat(perfil.getInsigniasObtenidas().add(new InsigniaObtenida(perfil, una))).isTrue();
            assertThat(perfil.getInsigniasObtenidas().add(new InsigniaObtenida(perfil, otra))).isFalse();
            assertThat(perfil.getInsigniasObtenidas()).hasSize(1);
        }

        @Test
        @DisplayName("dos insignias nuevas distintas no son iguales")
        void dosInsigniasNuevasDistintasNoSonIguales() {
            Insignia una = new Insignia("Medalla A", "desc");
            Insignia otra = new Insignia("Medalla A", "desc");

            assertThat(una).isNotEqualTo(otra);
        }

        private static Insignia insigniaConId(UUID id, String nombre) throws Exception {
            Insignia insignia = new Insignia(nombre, "desc");
            Field field = Insignia.class.getDeclaredField("idInsignia");
            field.setAccessible(true);
            field.set(insignia, id);
            return insignia;
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

}
