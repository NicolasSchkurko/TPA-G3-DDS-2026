package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La ingesta de donaciones tiene que ser idempotente (punto 14).
 *
 * <p>El escenario que motiva esto: {@code N8nClient} relanzaba su excepción después del
 * commit (punto 13), así que el donante veía un 500 con la transacción ya confirmada.
 * Cualquier cliente HTTP reintenta por defecto, y la segunda pasada insertaba otra fila y
 * volvía a aplicar la regla: progreso inflado y, en el peor caso, una insignia antes de
 * tiempo.
 *
 * <p>La clave es que {@code ImpactoDonacion.idDonacion} es el id de la donación en el
 * servicio de origen y además la primary key local, así que la deduplicación es un
 * {@code findById} y no hace falta comparar el contenido.
 */
@DisplayName("PerfilService: idempotencia de la ingesta de donaciones")
class PerfilServiceIdempotenciaTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID ID_DONACION = UUID.randomUUID();
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 3, 10, 10, 0);

    private RepositorioPerfiles repoPerfiles;
    private RepositorioDonaciones repoDonaciones;
    private PerfilService service;

    @BeforeEach
    void setUp() {
        repoPerfiles = mock(RepositorioPerfiles.class);
        repoDonaciones = mock(RepositorioDonaciones.class);
        service = new PerfilService(
                repoPerfiles,
                mock(RepositorioCategorias.class),
                repoDonaciones
        );
    }

    private ImpactoDonacionDTO dto() {
        ImpactoDonacionDTO dto = new ImpactoDonacionDTO();
        dto.setIdDonacion(ID_DONACION);
        dto.setFechaEntrega(FECHA);
        dto.setCantidadBienes(4);
        dto.setCategoria("INDUMENTARIA");
        dto.setSubCategoria("ROPA");
        dto.setEntidadBeneficiaria("Fundacion");
        dto.setEstado("ENTREGADA");
        return dto;
    }

    /** La fila que quedó guardada la primera vez. */
    private ImpactoDonacion donacionGuardada(boolean completoMision) {
        ImpactoDonacion donacion = new ImpactoDonacion(
                ID_DONACION, USUARIO,
                "Fundacion", 4, FECHA, "INDUMENTARIA", "ROPA", "ENTREGADA");
        donacion.registrarSiCompletoMision(completoMision);
        return donacion;
    }

    /** Un perfil sin misión activa: alcanza para verificar que la donación se guarda. */
    private void hayPerfilRegistrado() {
        when(repoPerfiles.findByIdUsuario(USUARIO)).thenReturn(Optional.of(new Perfil(USUARIO, "Ana")));
    }

    @Test
    @DisplayName("una donación repetida no se vuelve a procesar")
    void unaDonacionRepetidaNoSeProcesa() {
        when(repoDonaciones.findById(ID_DONACION))
                .thenReturn(Optional.of(donacionGuardada(true)));

        boolean resultado = service.actualizarPerfilImpacto(USUARIO, dto());

        // Lo importante es que no se guarda una segunda vez ni se toca el perfil: si se
        // reprocesara, volvería a aplicar la regla y el progreso quedaría inflado.
        assertThat(resultado).isTrue();
        verify(repoDonaciones, never()).save(any());
        verify(repoPerfiles, never()).save(any());
    }

    @Test
    @DisplayName("el reintento devuelve el mismo resultado que la primera vez")
    void elReintentoDevuelveElMismoResultado() {
        when(repoDonaciones.findById(ID_DONACION))
                .thenReturn(Optional.of(donacionGuardada(true)));
        boolean primeraVez = service.actualizarPerfilImpacto(USUARIO, dto());

        when(repoDonaciones.findById(ID_DONACION))
                .thenReturn(Optional.of(donacionGuardada(true)));
        Boolean segundaVez = service.actualizarPerfilImpacto(USUARIO, dto());

        // Un endpoint idempotente no puede devolver algo distinto la segunda vez: el
        // cliente ya recibió true, y si ahora recibiera false lo tomaría por un fallo.
        assertThat(segundaVez).isEqualTo(primeraVez).isTrue();
    }

    @Test
    @DisplayName("el reintento de una donación que NO completó misión también es idempotente")
    void elReintentoDeUnaDonacionIncompletaTambienEsIdempotente() {
        when(repoDonaciones.findById(ID_DONACION))
                .thenReturn(Optional.of(donacionGuardada(false)));

        boolean resultado = service.actualizarPerfilImpacto(USUARIO, dto());

        assertThat(resultado).isFalse();
        verify(repoDonaciones, never()).save(any());
    }

    @Test
    @DisplayName("una donación nueva sí se procesa y guarda con el id de origen")
    void unaDonacionNuevaSiSeProcesa() {
        hayPerfilRegistrado();
        when(repoDonaciones.findById(ID_DONACION)).thenReturn(Optional.empty());

        service.actualizarPerfilImpacto(USUARIO, dto());

        verify(repoDonaciones, times(1)).save(any(ImpactoDonacion.class));
    }

    @Test
    @DisplayName("el id de la donating guardada es el de origen, sin traducir")
    void elIdGuardadoEsElDeOrigen() {
        hayPerfilRegistrado();
        when(repoDonaciones.findById(ID_DONACION)).thenReturn(Optional.empty());

        service.actualizarPerfilImpacto(USUARIO, dto());

        // La primary key local ES el id de la donación en donaciones-service: eso es lo
        // que hace que el findById sirva para detectar el reintento.
        var captor = org.mockito.ArgumentCaptor.forClass(ImpactoDonacion.class);
        verify(repoDonaciones).save(captor.capture());
        assertThat(captor.getValue().getIdDonacion()).isEqualTo(ID_DONACION);
    }

    @Test
    @DisplayName("dos donaciones distintas del mismo donante se procesan por separado")
    void dosDonacionesDistintasSeProcesanPorSeparado() {
        hayPerfilRegistrado();
        when(repoDonaciones.findById(ID_DONACION)).thenReturn(Optional.empty());

        ImpactoDonacionDTO otra = dto();
        otra.setIdDonacion(UUID.randomUUID());
        otra.setCantidadBienes(9);
        UUID idOtra = UUID.randomUUID();

        when(repoDonaciones.findById(idOtra)).thenReturn(Optional.empty());

        service.actualizarPerfilImpacto(USUARIO, dto());
        service.actualizarPerfilImpacto(USUARIO, otra);

        // Que el id sea el de origen evita el problema opuesto: tratar como reintento dos
        // dones realmente distintas del mismo donante.
        verify(repoDonaciones, times(2)).save(any(ImpactoDonacion.class));
    }
}
