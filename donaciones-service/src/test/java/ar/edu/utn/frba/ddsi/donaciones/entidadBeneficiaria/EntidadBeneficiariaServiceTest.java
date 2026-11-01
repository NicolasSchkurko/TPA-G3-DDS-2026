package ar.edu.utn.frba.ddsi.donaciones.entidadBeneficiaria;

import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadExtraordinaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.TipoJuridico;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.*;
import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EntidadBeneficiariaServiceTest {

    private RepositorioPersonas repositorioPersonas;
    private RepositorioSubcategoriasDeBienes repositorioSubcategoriasDeBienes;
    private RepositorioCiudades repositorioCiudades;
    private RepositorioEntidadesBeneficiarias repositorioEntidadesBeneficiarias;
    private RepositorioNecesidades repositorioNecesidades;
    private EntidadBeneficiariaService service;

    @BeforeEach
    void setUp() {
        repositorioPersonas = mock(RepositorioPersonas.class);
        repositorioSubcategoriasDeBienes = mock(RepositorioSubcategoriasDeBienes.class);
        repositorioCiudades = mock(RepositorioCiudades.class);
        repositorioEntidadesBeneficiarias = mock(RepositorioEntidadesBeneficiarias.class);
        repositorioNecesidades = mock(RepositorioNecesidades.class);

        service = new EntidadBeneficiariaService(
                repositorioPersonas,
                repositorioSubcategoriasDeBienes,
                repositorioCiudades,
                repositorioEntidadesBeneficiarias,
                repositorioNecesidades
        );
    }

    @Test
    @DisplayName("Punto 70: eliminarEntidad aplica Soft Delete, cancela necesidades y anonimiza sin borrado físico")
    void eliminarEntidad_aplicaSoftDelete_yCancelaNecesidades_sinBorradoFisico() {
        UUID entidadId = UUID.randomUUID();

        Pais pais = new Pais("Argentina");
        Provincia provincia = new Provincia("CABA", pais);
        Ciudad ciudad = new Ciudad("CABA", provincia);
        Direccion direccion = new Direccion("Av. San Juan", "Boedo", 1234, 1, "A", ciudad);

        Juridica juridica = new Juridica("Comedor Esperanza", "Alimentacion", TipoJuridico.ONG, "30-11111111-9", new ArrayList<>());
        EntidadBeneficiaria entidad = new EntidadBeneficiaria(direccion, juridica);
        entidad.setId(entidadId);

        CategoriaBien alimentos = new CategoriaBien("Alimentos");
        SubcategoriaBien leche = new SubcategoriaBien("Leche", alimentos);
        Necesidad necesidad = new NecesidadExtraordinaria(leche, "Leche en polvo", 50);
        entidad.agregarNecesidad(necesidad);

        when(repositorioEntidadesBeneficiarias.buscarPorId(entidadId)).thenReturn(Optional.of(entidad));

        assertTrue(entidad.isActiva());
        assertTrue(necesidad.isActiva());

        service.eliminarEntidad(entidadId);

        // Verificaciones de estado soft-delete
        assertFalse(entidad.isActiva());
        assertNotNull(entidad.getFechaBaja());
        assertFalse(necesidad.isActiva());

        // Verificaciones de anonimización
        assertNull(entidad.getPersonaJuridica().getRazonSocial());
        assertNull(entidad.getPersonaJuridica().getCuit());
        assertNull(entidad.getDireccion().getCalleUno());

        // Verificaciones de repositorio: se actualiza y sincroniza, NUNCA se borra físicamente
        verify(repositorioEntidadesBeneficiarias).actualizar(eq(entidadId), eq(entidad));
        verify(repositorioEntidadesBeneficiarias).flush();
        verify(repositorioEntidadesBeneficiarias, never()).eliminarPorId(any());
    }

    @Test
    @DisplayName("Punto 70: eliminarEntidad con ID inexistente no hace nada ni falla")
    void eliminarEntidad_siNoExiste_noFalla() {
        UUID entidadId = UUID.randomUUID();
        when(repositorioEntidadesBeneficiarias.buscarPorId(entidadId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.eliminarEntidad(entidadId));

        verify(repositorioEntidadesBeneficiarias, never()).actualizar(any(), any());
        verify(repositorioEntidadesBeneficiarias, never()).eliminarPorId(any());
    }

    @Test
    @DisplayName("Punto 70: obtenerTodas() excluye a las entidades dadas de baja (activa = false)")
    void obtenerTodas_excluyeEntidadesInactivas() {
        EntidadBeneficiaria activa = new EntidadBeneficiaria();
        EntidadBeneficiaria inactiva = new EntidadBeneficiaria();
        inactiva.darDeBaja();

        when(repositorioEntidadesBeneficiarias.obtenerTodas()).thenReturn(List.of(activa, inactiva));

        List<EntidadBeneficiariaDTO> resultado = service.obtenerTodas();

        assertEquals(1, resultado.size());
        assertEquals(activa.getId(), resultado.get(0).getId());
        assertTrue(resultado.get(0).getActiva());
    }

    @Test
    @DisplayName("Punto 70: eliminarNecesidad desactiva lógicamente la necesidad sin borrado físico")
    void eliminarNecesidad_cancelaLogicamenteSinBorradoFisico() {
        UUID entidadId = UUID.randomUUID();
        UUID necesidadId = UUID.randomUUID();

        EntidadBeneficiaria entidad = new EntidadBeneficiaria();
        entidad.setId(entidadId);

        CategoriaBien alimentos = new CategoriaBien("Alimentos");
        SubcategoriaBien leche = new SubcategoriaBien("Leche", alimentos);
        Necesidad necesidad = new NecesidadExtraordinaria(leche, "Leche en polvo", 50);
        necesidad.setId(necesidadId);
        entidad.agregarNecesidad(necesidad);

        when(repositorioEntidadesBeneficiarias.buscarPorId(entidadId)).thenReturn(Optional.of(entidad));

        assertTrue(necesidad.isActiva());

        service.eliminarNecesidad(entidadId, necesidadId);

        assertFalse(necesidad.isActiva());
        verify(repositorioNecesidades).actualizar(eq(necesidadId), eq(necesidad));
        verify(repositorioNecesidades).flush();
        verify(repositorioNecesidades, never()).eliminarPorId(any());
    }

    @Test
    @DisplayName("Punto 70: obtenerNecesidades excluye necesidades inactivas")
    void obtenerNecesidades_excluyeNecesidadesInactivas() {
        UUID entidadId = UUID.randomUUID();
        EntidadBeneficiaria entidad = new EntidadBeneficiaria();
        entidad.setId(entidadId);

        CategoriaBien alimentos = new CategoriaBien("Alimentos");
        SubcategoriaBien leche = new SubcategoriaBien("Leche", alimentos);

        Necesidad activa = new NecesidadExtraordinaria(leche, "Activa", 50);
        Necesidad inactiva = new NecesidadExtraordinaria(leche, "Inactiva", 50);
        inactiva.setActiva(false);

        entidad.agregarNecesidad(activa);
        entidad.agregarNecesidad(inactiva);

        when(repositorioEntidadesBeneficiarias.buscarPorId(entidadId)).thenReturn(Optional.of(entidad));

        List<NecesidadDTO> necesidades = service.obtenerNecesidades(entidadId);

        assertEquals(1, necesidades.size());
        assertEquals(activa.getId(), necesidades.get(0).getId());
    }
}
