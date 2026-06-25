package ar.edu.utn.frba.ddsi.incentivos.Persona;

import ar.edu.utn.frba.ddsi.incentivos.dto.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.TipoCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.services.PersonaService;
import ar.edu.utn.frba.ddsi.incentivos.clients.NotificacionClient;
import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.clients.N8nClient;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.PerfilDuplicadoException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.PerfilInexistenteException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;




import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class PersonaServiceTest {

    @Mock
    private NotificacionClient notificacionClient;

    @Mock
    private DonacionClient donacionClient;

    @Mock
    private N8nClient n8nClient;

    @Mock
    private RepositorioDonaciones repositorioDonaciones;

    @Mock
    private RepositorioPerfiles repositorioPerfiles;

    @Mock
    private RepositorioCategorias repositorioCategorias;

    @InjectMocks
    private PersonaService personaService;

    @Test
    public void crearPerfil_success() {
        UUID id = UUID.randomUUID();
        PerfilDonanteDTO dto = new PerfilDonanteDTO(id, "nombre");

        when(repositorioPerfiles.buscarPorIDUsuario(id)).thenReturn(null);
        when(repositorioCategorias.buscarPorTipo(any())).thenReturn(null);

        PerfilDTO resultado = personaService.crearPerfil(dto);

        assertEquals("nombre", resultado.getNombreUsuario());
        assertEquals(TipoCategoria.COLABORADOR.name(), resultado.getCategoriaActual());
        verify(repositorioPerfiles).agregarPerfil(any(Perfil.class));
    }

    @Test
    public void crearPerfil_datosInvalidos() {
        PerfilDonanteDTO dto = new PerfilDonanteDTO(null, "nombre");
        assertThrows(DatosInvalidosException.class, () -> personaService.crearPerfil(dto));
    }

    @Test
    public void crearPerfil_duplicado() {
        UUID id = UUID.randomUUID();
        PerfilDonanteDTO dto = new PerfilDonanteDTO(id, "nombre");
        when(repositorioPerfiles.buscarPorIDUsuario(id)).thenReturn(new Perfil(id, "x"));

        assertThrows(PerfilDuplicadoException.class, () -> personaService.crearPerfil(dto));
    }

    @Test
    public void convertirDTO_mapsFields() {
        UUID id = UUID.randomUUID();
        ImpactoDonacionDTO dto = new ImpactoDonacionDTO();
        dto.setIdUsuario(id);
        dto.setFechaEntrega(LocalDate.of(2023,1,1));
        dto.setCantidadBienes(5);
        dto.setCategoria("cat");
        dto.setSubCategoria("sub");
        dto.setEntidadBeneficiaria("entidad");
        dto.setEstado("ENTREGADA");

        ImpactoDonacion imp = personaService.convertirDTO(dto);

        assertEquals(id, imp.getIdUsuario());
        assertEquals(5, imp.getCantidadBienes());
        assertEquals("cat", imp.getCategoria());
        assertEquals("sub", imp.getSubCategoria());
        assertEquals("entidad", imp.getEntidadBeneficiaria());
        assertEquals("ENTREGADA", imp.getEstado());
        assertEquals(LocalDate.of(2023,1,1), imp.getFechaEntrega());
    }

    @Test
    public void actualizarPerfil_datosInvalidos() {
        ImpactoDonacionDTO dto = new ImpactoDonacionDTO();
        dto.setIdUsuario(null);

        assertThrows(DatosInvalidosException.class, () -> personaService.actualizarPerfil(dto));
    }

    @Test
    public void actualizarPerfil_perfilInexistente() {
        UUID id = UUID.randomUUID();
        ImpactoDonacionDTO dto = new ImpactoDonacionDTO();
        dto.setIdUsuario(id);

        when(repositorioPerfiles.buscarPorIDUsuario(id)).thenReturn(null);

        assertThrows(PerfilInexistenteException.class, () -> personaService.actualizarPerfil(dto));
    }
}
