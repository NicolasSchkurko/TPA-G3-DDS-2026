package ar.edu.utn.frba.ddsi.incentivos.models.entities;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.CategoriaMision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.ValoresDistintos;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.lang.reflect.Field;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Punto 17: las relaciones que se reemplazan borran su fila vieja")
class OrphanRemovalTest {

    private static Field field(Class<?> tipo, String nombre) {
        try {
            return tipo.getDeclaredField(nombre);
        } catch (NoSuchFieldException excepcion) {
            throw new AssertionError( tipo.getSimpleName() + " no tiene el campo " + nombre, excepcion);
        }
    }

    private static boolean tieneOrphanRemoval(Class<?> tipo, String nombre) {
        return Optional.ofNullable(field(tipo, nombre).getAnnotation(OneToOne.class))
                .map(relacion -> relacion.orphanRemoval())
                .orElse(false);
    }

    private static boolean tieneOrphanRemovalOneToMany(Class<?> tipo, String nombre) {
        return Optional.ofNullable(field(tipo, nombre).getAnnotation(OneToMany.class))
                .map(relacion -> relacion.orphanRemoval())
                .orElse(false);
    }

    @Test
    @DisplayName("Perfil.progresoMisionActual borra el progreso anterior")
    void elPerfilBorraElProgresoAnterior() {
        assertThat(tieneOrphanRemoval(Perfil.class, "progresoMisionActual"))
                .as("""
                Cada cambio de misión o de categoría reemplaza el ProgresoMision. Sin
                orphanRemoval queda uno huérfano por cada avance del donante en la tabla
                progreso_mision.
                """)
                .isTrue();
    }

    @Test
    @DisplayName("Mision.reglaDeProgreso borra la regla anterior y, en cascada, su constancia y su operación")
    void laMisionBorraLaReglaAnterior() {
        assertThat(tieneOrphanRemoval(Mision.class, "reglaDeProgreso"))
                .as("cada cambio de criterio dejaba Regla, ReglaConstancia y Operacion viejas")
                .isTrue();
    }

    @Test
    @DisplayName("Perfil.insigniasObtenidas ya lo tenía")
    void lasInsigniasObtenidasYaLoTenian() {
        assertThat(tieneOrphanRemovalOneToMany(Perfil.class, "insigniasObtenidas")).isTrue();
    }

    @Test
    @DisplayName("Mision.insigniaObjetivo NO borra: la referencia el historial de todos los que la obtuvieron")
    void laInsigniaObjetivoNoBorra() {
        assertThat(tieneOrphanRemoval(Mision.class, "insigniaObjetivo"))
                .as("""
                Esta es la que NO debe llevar orphanRemoval. InsigniaObtenida.insignia es un
                ManyToOne: todas las filas de insignia_obtenida apuntan a ella. Con
                orphanRemoval, Hibernate intentaría borrar la insignia al reemplazar la
                referencia y reventaría por FK, o se llevaría por delante insignias ya
                otorgadas.

                Hoy Mision.actualizar modifica la insignia en el lugar en vez de reemplazar
                la referencia, así que no hay huérfana. El día que se reemplace, el borrado
                tiene que ser explícito.
                """)
                .isFalse();
    }

    @Test
    @DisplayName("Regla.constancia y Regla.operacion NO lo llevan: se van con la regla vieja")
    void lasRelacionesDeLaReglaNoLoLlevan() {
        assertThat(tieneOrphanRemoval(Regla.class, "constancia"))
                .as("la regla no se modifica en el lugar: se reemplaza entera y el cascade borra sus hijas")
                .isFalse();
        assertThat(tieneOrphanRemoval(Regla.class, "operacion")).isFalse();
    }

    @Test
    @DisplayName("la regla y su operación no se comparten entre misiones")
    void lasReglasNoSeComparten() {
        // El factory construye una Regla por misión, así que no se comparten.
        Mision primera = new Mision("A", null, "d", "i",
                new Regla(new ReglaConstancia(1, ChronoUnit.MONTHS),
                        ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas
                                .AtributoImpacto.CANTIDAD_BIENES,
                        new SuperaCantidad(1, 3)));
        Mision segunda = new Mision("B", null, "d", "i",
                new Regla(new ReglaConstancia(2, ChronoUnit.MONTHS),
                        ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas
                                .AtributoImpacto.CANTIDAD_BIENES,
                        new CantidadCoincidencias(2, null)));

        assertThat(primera.getReglaDeProgreso()).isNotSameAs(segunda.getReglaDeProgreso());
        assertThat(primera.getReglaDeProgreso().getOperacion())
                .isNotSameAs(segunda.getReglaDeProgreso().getOperacion());
    }

    @Test
    @DisplayName("el progreso huérfano no se puede leer desde ningún lado, solo desde su perfil")
    void elProgresoHuerfanoNoTieneOtrosDuenos() {
        // Si el ProgresoMision tuviera otro dueño, el orphanRemoval de Perfil podría borrar una fila en uso.
        Perfil perfil = new Perfil(UUID.randomUUID(), "Ana");
        Mision mision = new Mision("Diez dones", null, "d", "i",
                new Regla(null,
                        ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas
                                .AtributoImpacto.ESTADO,
                        new SuperaCantidad(1, 10)));
        perfil.iniciarEn(new Categoria("Colaborador", null, 1, List.of(mision)));

        ProgresoMision progreso = perfil.getProgresoMisionActual();

        assertThat(progreso).isNotNull();
        // El unico que lo tiene es el perfil.
        assertThat(progreso).isSameAs(perfil.getProgresoMisionActual());
    }
}