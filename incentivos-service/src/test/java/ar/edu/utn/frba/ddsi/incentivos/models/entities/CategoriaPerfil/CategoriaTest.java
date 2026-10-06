package ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.OperacionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import java.util.ArrayList;
import java.util.List;
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
    @DisplayName("la ultima mision de la secuencia no tiene siguiente")
    void laUltimaMisionNoTieneSiguiente() {
        // No hace falta un esUltimaMision: que siguienteMision devuelva null YA es la
        // forma de preguntar si la donante es la ultima.
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
    @DisplayName("ignora los nulls al agregar o eliminar misiones")
    void ignoraNulls() {
        Categoria categoria = new Categoria("Vacia", null, 1, new ArrayList<>());

        categoria.agregarMision(null);
        categoria.eliminarMision(null);

        assertThat(categoria.getCategoriaMisiones()).isEmpty();
    }
}
