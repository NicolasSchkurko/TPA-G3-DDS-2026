package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MisionDTO: la proyeccion no se rompe cuando a la entidad le falta algo")
class MisionDTOTest {

    private static Mision mision(Regla regla) {
        return new Mision("Diez dones", UUID.randomUUID(), "Donar diez veces", "Constante", regla);
    }

    @Test
    @DisplayName("proyecta nombre, insignia, constancia y operacion")
    void proyectaLaMisionCompleta() {
        MisionDTO dto = MisionDTO.desdeEntidad(mision(new Regla(
                new ReglaConstancia(1, ChronoUnit.MONTHS),
                AtributoImpacto.ESTADO,
                new SuperaCantidad(10, 3)
        )));

        assertThat(dto.getNombreMision()).isEqualTo("Diez dones");
        assertThat(dto.getDescripcion()).isEqualTo("Donar diez veces");
        assertThat(dto.getInsigniaObjetivo()).isEqualTo("Constante");
        assertThat(dto.getRegla().getAtributo()).isEqualTo("ESTADO");
        assertThat(dto.getRegla().getConstancia().getCantidad()).isEqualTo(1);
        assertThat(dto.getRegla().getConstancia().getUnidadTiempo()).isEqualTo("MONTHS");
        assertThat(dto.getRegla().getOperacion().getTipoOperacion()).isEqualTo("SUPERA_CANTIDAD");
        assertThat(dto.getRegla().getOperacion().getProgresoObjetivo()).isEqualTo(10);
        assertThat(dto.getRegla().getOperacion().getCantidad()).isEqualTo(3);
    }

    @Test
    @DisplayName("una regla vacia no rompe el listado completo de misiones")
    void unaReglaVaciaNoRompeElListado() {
        MisionDTO dto = MisionDTO.desdeEntidad(mision(new Regla(null, null, null)));

        assertThat(dto).isNotNull();
        assertThat(dto.getRegla()).isNotNull();
        assertThat(dto.getRegla().getAtributo()).isNull();
        assertThat(dto.getRegla().getConstancia()).isNull();
        assertThat(dto.getRegla().getOperacion()).isNull();
    }

    @Test
    @DisplayName("una insignia nula no rompe la proyeccion")
    void unaInsigniaNulaNoRompeLaProyeccion() {
        Mision conInsignia = mision(new Regla(null, AtributoImpacto.ESTADO, new SuperaCantidad(1, 1)));
        conInsignia.setInsigniaObjetivo(null);

        MisionDTO dto = MisionDTO.desdeEntidad(conInsignia);

        assertThat(dto).isNotNull();
        assertThat(dto.getInsigniaObjetivo()).isNull();
    }

    @Test
    @DisplayName("una constancia sin unidad de tiempo queda en null y no en el texto 'null'")
    void unaConstanciaSinUnidadQuedaEnNull() {
        MisionDTO dto = MisionDTO.desdeEntidad(mision(new Regla(
                new ReglaConstancia(5, null),
                AtributoImpacto.ESTADO,
                new SuperaCantidad(1, 1)
        )));

        assertThat(dto.getRegla().getConstancia().getCantidad()).isEqualTo(5);
        assertThat(dto.getRegla().getConstancia().getUnidadTiempo()).isNull();
    }

    @Test
    @DisplayName("una coincidencia sin valor esperado devuelve null y no el texto 'null'")
    void unaCoincidenciaSinValorEsperadoDevuelveNull() {
        OperacionDTO dto = OperacionDTO.desdeEntidad(new CantidadCoincidencias(3, null));

        assertThat(dto.getTipoOperacion()).isEqualTo("COINCIDENCIAS");
        assertThat(dto.getValorEsperado()).isNull();
    }

    @Test
    @DisplayName("proyectar una mision nula devuelve null en vez de reventar")
    void proyectarUnaMisionNulaDevuelveNull() {
        assertThat(MisionDTO.desdeEntidad(null)).isNull();
        assertThat(OperacionDTO.desdeEntidad(null)).isNull();
    }
}