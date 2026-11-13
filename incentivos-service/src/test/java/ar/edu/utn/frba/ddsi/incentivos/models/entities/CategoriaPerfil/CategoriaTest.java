package ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.OperacionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Categoria: secuencia ordenada de misiones")
class CategoriaTest {

    private MisionFactory misionFactory;

    @BeforeEach
    void setUp() {
        misionFactory = new MisionFactory(new OperacionFactory());
    }

    private Mision mision(String nombre) {
        return misionFactory.crearMision(
                null,                    // idAdmin
                nombre,                  // nombreMision
                "Descripcion",           // descripcion
                "Insignia " + nombre,    // nombreInsignia
                "Texto de la insignia",  // insigniaDescripcion
                null,                    // insigniaUrlImagen
                null,                    // cantidadTiempo
                null,                    // unidadTiempo
                "ESTADO",                // atributo
                "COINCIDENCIAS",         // tipoOperacion
                1,                       // progresoObjetivo
                null,                    // cantidadOperacion
                "ENTREGADA"              // valorEsperado
        );
    }

    private Categoria categoriaCon(Mision... misiones) {
        return new Categoria("Categoria", null, 1, new ArrayList<>(List.of(misiones)));
    }

    @Test
    @DisplayName("agrega las misiones con posiciones correlativas desde uno")
    void agregaMisionesConPosicionesCorrelativas() {
        Mision a = mision("A");
        Mision b = mision("B");
        Mision c = mision("C");

        Categoria categoria = categoriaCon(a, b, c);

        assertThat(categoria.getCategoriaMisiones()).hasSize(3);
        assertThat(categoria.getCategoriaMisiones())
                .extracting(CategoriaMision::getPosicion).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("expone la primera y la siguiente mision de la secuencia")
    void navegaLaSecuenciaDeMisiones() {
        Mision a = mision("A");
        Mision b = mision("B");
        Categoria categoria = categoriaCon(a, b);

        assertThat(categoria.primeraMision()).isSameAs(a);
        assertThat(categoria.siguienteMision(a)).isSameAs(b);
        assertThat(categoria.siguienteMision(b)).isNull();
    }

    @Test
    @DisplayName("reindexa las posiciones al eliminar una mision del medio")
    void reindexaAlEliminarUnaMisionDelMedio() {
        Mision a = mision("A");
        Mision b = mision("B");
        Mision c = mision("C");
        Categoria categoria = categoriaCon(a, b, c);

        categoria.eliminarMision(b);

        assertThat(categoria.getCategoriaMisiones()).hasSize(2);
        assertThat(categoria.getCategoriaMisiones())
                .extracting(CategoriaMision::getPosicion).containsExactly(1, 2);
        assertThat(categoria.primeraMision()).isSameAs(a);
        assertThat(categoria.siguienteMision(a)).isSameAs(c);
    }

    @Test
    @DisplayName("permite quitar todas las misiones al editar una categoría")
    void permiteQuitarTodasLasMisionesAlEditar() {
        Categoria categoria = categoriaCon(mision("A"), mision("B"));

        categoria.actualizarCon(new Categoria("Categoria", null, 1, List.of()));

        assertThat(categoria.getCategoriaMisiones()).isEmpty();
    }

    @Test
    @DisplayName("la ultima mision de la secuencia no tiene siguiente")
    void laUltimaMisionNoTieneSiguiente() {
        // No hace falta un esUltimaMision: siguienteMision == null ya responde eso.
        Mision a = mision("A");
        Mision b = mision("B");
        Categoria categoria = categoriaCon(a, b);

        assertThat(categoria.siguienteMision(a)).isSameAs(b);
        assertThat(categoria.siguienteMision(b)).isNull();
    }

    @Test
    @DisplayName("sin misiones no rompe las consultas de secuencia")
    void sinMisionesNoRompeLaSecuencia() {
        Categoria categoria = new Categoria("Vacia", null, 1, new ArrayList<>());

        assertThat(categoria.primeraMision()).isNull();
        assertThat(categoria.siguienteMision(mision("A"))).isNull();
        categoria.eliminarMision(mision("A"));
        assertThat(categoria.getCategoriaMisiones()).isEmpty();
    }

    @Test
    @DisplayName("encuentra la siguiente aunque la colección tenga un proxy de Hibernate")
    void siguienteMisionToleraProxy() {
        UUID idA = UUID.randomUUID();
        Mision proxyA = mock(Mision.class);
        Mision cargadaA = mock(Mision.class);
        when(proxyA.getIdMision()).thenReturn(idA);
        when(cargadaA.getIdMision()).thenReturn(idA);
        Mision b = mision("B");

        Categoria categoria = categoriaCon(proxyA, b);

        // El proxy y la entidad cargada son instancias distintas con el mismo id.
        assertThat(categoria.siguienteMision(cargadaA)).isSameAs(b);
    }

    @Test
    @DisplayName("elimina la misión aunque la colección tenga un proxy de Hibernate")
    void eliminarMisionToleraProxy() {
        UUID idA = UUID.randomUUID();
        Mision proxyA = mock(Mision.class);
        Mision cargadaA = mock(Mision.class);
        when(proxyA.getIdMision()).thenReturn(idA);
        when(cargadaA.getIdMision()).thenReturn(idA);
        Mision b = mision("B");

        Categoria categoria = categoriaCon(proxyA, b);
        categoria.eliminarMision(cargadaA);

        assertThat(categoria.getCategoriaMisiones()).hasSize(1);
        assertThat(categoria.primeraMision()).isSameAs(b);
    }

    @Test
    @DisplayName("no confunde dos misiones nuevas distintas")
    void noConfundeDosMisionesNuevasDistintas() {
        // Sin id no hay identidad persistente: dos misiones nuevas diferentes no son la misma.
        Mision a = mision("A");
        Mision otra = mision("A");
        Categoria categoria = categoriaCon(a);

        assertThat(categoria.siguienteMision(otra)).isNull();
        categoria.eliminarMision(otra);
        assertThat(categoria.getCategoriaMisiones()).hasSize(1);
    }

    @Test
    @DisplayName("ignora los nulls al agregar o eliminar misiones")
    void ignoraNulls() {
        Categoria categoria = new Categoria("Vacia", null, 1, new ArrayList<>());

        categoria.agregarMision(null);
        categoria.eliminarMision(null);

        assertThat(categoria.getCategoriaMisiones()).isEmpty();
    }
}
