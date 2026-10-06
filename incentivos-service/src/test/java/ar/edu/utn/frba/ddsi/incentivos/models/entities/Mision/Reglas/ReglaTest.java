package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.ProgresoDelDonante;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Regla: composicion de atributo + operacion (+ constancia)")
class ReglaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final UUID USUARIO = UUID.randomUUID();

    /** La regla recibe el avance del donante: sin esto no podria evaluar reglas con memoria. */
    private static final ProgresoDelDonante DONANTE = new ProgresoMision(null);

    private static ImpactoDonacion donacion(String categoria, String subCategoria,
                                            Integer cantidadBienes, String estado) {
        return new ImpactoDonacion(
                UUID.randomUUID(),
                USUARIO,
                "Fundacion de prueba",
                cantidadBienes,
                LocalDateTime.of(2026, 3, 10, 12, 0),
                categoria,
                subCategoria,
                estado
        );
    }

    @Test
    @DisplayName("el mapeo del constructor conserva categoria y subCategoria en su campo")
    void mapeaCategoriaYSubCategoriaEnElCampoCorrecto() {
        ImpactoDonacion impacto = donacion("INDUMENTARIA", "ROPA", 3, "ENTREGADA");

        assertThat(impacto.getCategoria()).isEqualTo("INDUMENTARIA");
        assertThat(impacto.getSubCategoria()).isEqualTo("ROPA");
    }

    @ParameterizedTest
    @EnumSource(AtributoImpacto.class)
    @DisplayName("aplicar devuelve el atributo correspondiente del impacto")
    void aplicaElAtributoCorrespondiente(AtributoImpacto atributo) {
        ImpactoDonacion impacto = donacion("ALIMENTOS", "MERCEARIA", 7, "ENTREGADA");
        LocalDateTime fecha = impacto.getFechaEntrega();

        Regla regla = new Regla(null, atributo, new SuperaCantidad(1, 1));

        Object valor = switch (atributo) {
            case ESTADO -> impacto.getEstado();
            case CATEGORIA -> impacto.getCategoria();
            case SUBCATEGORIA -> impacto.getSubCategoria();
            case CANTIDAD_BIENES -> impacto.getCantidadBienes();
            case ENTIDAD -> impacto.getEntidadBeneficiaria();
            case FECHA -> fecha;
        };

        assertThat(regla.aplicar(impacto)).isEqualTo(valor);
    }

    @Test
    @DisplayName("una regla sobre CATEGORIA lee la categoria, no la subcategoria")
    void unaReglaDeCategoriaNoLeeLaSubcategoria() {
        Regla regla = new Regla(
                null,
                AtributoImpacto.CATEGORIA,
                new CantidadCoincidencias(1, MAPPER.valueToTree("ALIMENTOS"))
        );

        ImpactoDonacion conCategoriasDistintas =
                donacion("ALIMENTOS", "MERCEARIA", 1, "ENTREGADA");

        assertThat(regla.operar(regla.aplicar(conCategoriasDistintas), DONANTE)).isTrue();
    }

    @Test
    @DisplayName("operar delega en la operacion y estaComplita tambien")
    void operaYEvaluaLaCompletitud() {
        Regla regla = new Regla(null, AtributoImpacto.CANTIDAD_BIENES, new SuperaCantidad(2, 5));

        assertThat(regla.operar(6, DONANTE)).isTrue();
        assertThat(regla.estaCompleta(2, DONANTE)).isTrue();
        assertThat(regla.estaCompleta(1, DONANTE)).isFalse();
    }

    @Test
    @DisplayName("una regla sin constancia la expone como null")
    void exponeLaConstanciaComoNull() {
        Regla regla = new Regla(null, AtributoImpacto.ESTADO, new SuperaCantidad(1, 1));

        assertThat(regla.getConstancia()).isNull();
    }
}
