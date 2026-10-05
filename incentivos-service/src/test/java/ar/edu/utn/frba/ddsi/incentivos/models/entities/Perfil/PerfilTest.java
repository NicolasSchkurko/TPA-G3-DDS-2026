package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.OperacionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.events.CategoriaNuevaPublicar;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCompletada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Perfil: raiz de agregado")
class PerfilTest {

    private static final MisionFactory MISION_FACTORY = new MisionFactory(new OperacionFactory());
    private static final UUID ID_USUARIO = UUID.randomUUID();
    private static final MedioContacto CONTACTO = new MedioContacto("EMAIL", "a@b.com");

    private static Mision mision(String nombre) {
        Regla regla = new Regla(null, AtributoImpacto.CANTIDAD_BIENES, new SuperaCantidad(1, 5));
        return new Mision(nombre, null, "Descripcion", "Insignia " + nombre, regla);
    }

    private static ImpactoDonacion donacion() {
        return new ImpactoDonacion("Fundacion", 7,
                LocalDateTime.of(2026, 3, 1, 10, 0),
                "ALIMENTOS", "MERCEARIA", "ENTREGADA", ID_USUARIO);
    }

    private static Categoria categoria(String nombre, Mision... misiones) {
        return new Categoria(nombre, null, 1, new ArrayList<>(List.of(misiones)));
    }

    /**
     * {@code domainEvents()} es protected en AbstractAggregateRoot, asi que se accede
     * por reflexion para no ensuciar la entidad con metodos solo para test.
     */
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
        perfil.setProgresoMisionActual(new ProgresoMision(mision("Primera")));

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
        perfil.setProgresoMisionActual(new ProgresoMision(anterior));

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
        perfil.setCategoriaActual(origen);
        perfil.setProgresoMisionActual(new ProgresoMision(mision("Primera")));

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
        perfil.setCategoriaActual(origen);

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
        perfil.setProgresoMisionActual(new ProgresoMision(mision("Primera")));

        perfil.progresarMision(donacion(), List.of());

        InsigniaObtenida obtenida = perfil.getInsigniasObtenidas().iterator().next();
        assertThat(obtenida.getPerfil()).isSameAs(perfil);
        assertThat(obtenida.getFechaObtencion()).isNotNull();
        assertThat(obtenida.getInsignia()).isNotNull();
    }
}
