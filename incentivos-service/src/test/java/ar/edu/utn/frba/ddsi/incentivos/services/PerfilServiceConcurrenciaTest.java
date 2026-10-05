package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * El control de concurrencia (punto 36).
 *
 * <p>Sin {@code @Version}, dos donaciones del mismo donante que entran al mismo tiempo
 * pierden una: las dos leen el mismo progreso, las dos le suman uno y la segunda escritura
 * pisa a la primera. Y si las dos completaban la misión, cada una insertaba su
 * {@code InsigniaObtenida}, porque el {@code Set} en memoria de cada petición es distinto:
 * el donante quedaba con dos insignias y el ranking lo puntuaba doble.
 *
 * <p>La detección real la hace Hibernate al hacer el {@code UPDATE}, así que sin base de
 * datos no se puede probar. Lo que sí se prueba acá son las dos mitades que sí se pueden:
 * que la anotación esté puesta, y que el reintento del servicio exista y sea seguro.
 */
@DisplayName("Punto 36: @Version y reintento ante carrera de concurrencia")
class PerfilServiceConcurrenciaTest {

    private final RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
    private final RepositorioDonaciones repoDonaciones = mock(RepositorioDonaciones.class);

    /**
     * Un template que corre el callback las veces que se le pidan, y que puede fallar con
     * una carrera de concurrencia las primeras.
     */
    private TransactionTemplate templateQueFalla(int veces) {
        TransactionTemplate template = mock(TransactionTemplate.class);
        AtomicInteger intentos = new AtomicInteger();
        when(template.execute(any())).thenAnswer(invocacion -> {
            TransactionCallback<?> accion = invocacion.getArgument(0);
            if (intentos.incrementAndGet() <= veces) {
                throw new OptimisticLockingFailureException("perfil", (Throwable) null);
            }
            return accion.doInTransaction(null);
        });
        return template;
    }

    private static ImpactoDonacionDTO dto(UUID idDonacion) {
        ImpactoDonacionDTO dto = new ImpactoDonacionDTO();
        dto.setIdDonacion(idDonacion);
        dto.setFechaEntrega(LocalDateTime.of(2026, 3, 10, 10, 0));
        dto.setCantidadBienes(4);
        dto.setCategoria("INDUMENTARIA");
        dto.setSubCategoria("ROPA");
        dto.setEntidadBeneficiaria("Fundacion");
        dto.setEstado("ENTREGADA");
        return dto;
    }

    private PerfilService servicioCon(TransactionTemplate template) {
        return new PerfilService(
                repoPerfiles,
                mock(RepositorioCategorias.class),
                repoDonaciones,
                template);
    }

    @Test
    @DisplayName("Perfil tiene @Version: es la raíz del agregado, donde viven progreso, categoría e insignias")
    void perfilTieneVersion() throws Exception {
        Field version = Perfil.class.getDeclaredField("version");

        assertThat(version.getAnnotation(jakarta.persistence.Version.class))
                .as("""
                Sin @Version en Perfil, dos donaciones simultáneas se pisan el progreso y
                pueden duplicar la insignia. Ponerlo en ProgresoMision no serviría: dejaría
                sin cubrir el avance de categoría y el Set de insignias, que son parte del
                mismo agregado.
                """)
                .isNotNull();
    }

    @Test
    @DisplayName("Categoria también tiene @Version: es el otro leer-modificar-escribir del servicio")
    void categoriaTieneVersion() throws Exception {
        Field version = Categoria.class.getDeclaredField("version");

        assertThat(version.getAnnotation(jakarta.persistence.Version.class))
                .as("actualizarCategoria lee, arma una nueva y guarda: sin @Version pisa")
                .isNotNull();
    }

    @Test
    @DisplayName("el @Version no tiene setter: tocarlo a mano rompe la garantía")
    void elVersionNoTieneSetter() throws Exception {
        Field version = Perfil.class.getDeclaredField("version");

        assertThat(version.getAnnotation(lombok.Setter.class))
                .as("el campo es de Hibernate, igual que el id")
                .isNull();
    }

    @Test
    @DisplayName("una carrera se reintenta y la donación se aplica igual")
    void unaCarreraSeReintenta() {
        when(repoDonaciones.findById(any())).thenReturn(Optional.empty());
        when(repoPerfiles.findByIdUsuario(any()))
                .thenReturn(Optional.of(new Perfil(UUID.randomUUID(), "Ana")));
        when(repoPerfiles.save(any(Perfil.class))).thenAnswer(inv -> inv.getArgument(0));

        PerfilService service = servicioCon(templateQueFalla(1));

        // La primera vuelta pierde la carrera, la segunda entra limpia.
        assertThat(service.actualizarPerfilImpacto(UUID.randomUUID(), dto(UUID.randomUUID())))
                .isFalse();

        // save UNA vez, no dos: el intento que pierde la carrera falla al hacer el UPDATE,
        // o sea antes de llegar al save. Y eso es justamente lo que hace seguro el reintento:
        // no quedó nada guardado a medias.
        verify(repoPerfiles, times(1)).save(any(Perfil.class));
        verify(repoDonaciones, times(1)).save(any());
    }

    @Test
    @DisplayName("dos carreras seguidas también se resuelven: hay margen de reintentos")
    void dosCarrerasResuelven() {
        when(repoDonaciones.findById(any())).thenReturn(Optional.empty());
        when(repoPerfiles.findByIdUsuario(any()))
                .thenReturn(Optional.of(new Perfil(UUID.randomUUID(), "Ana")));
        when(repoPerfiles.save(any(Perfil.class))).thenAnswer(inv -> inv.getArgument(0));

        PerfilService service = servicioCon(templateQueFalla(2));

        assertThat(service.actualizarPerfilImpacto(UUID.randomUUID(), dto(UUID.randomUUID())))
                .isFalse();

        // Solo el tercer intento llegó a escribir.
        verify(repoPerfiles, times(1)).save(any(Perfil.class));
    }

    @Test
    @DisplayName("agotados los reintentos, la excepción sube para que el handler la mapee a 409")
    void agotadosLosReintentosLaExcepcionSube() {
        when(repoDonaciones.findById(any())).thenReturn(Optional.empty());
        when(repoPerfiles.findByIdUsuario(any()))
                .thenReturn(Optional.of(new Perfil(UUID.randomUUID(), "Ana")));

        PerfilService service = servicioCon(templateQueFalla(99));

        assertThatThrownBy(() ->
                service.actualizarPerfilImpacto(UUID.randomUUID(), dto(UUID.randomUUID())))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("una excepción que NO es de carrera no se reintenta")
    void unaExcepcionAjenaNoSeReintenta() {
        TransactionTemplate template = mock(TransactionTemplate.class);
        when(template.execute(any())).thenThrow(new IllegalStateException("otra cosa"));

        PerfilService service = servicioCon(template);

        assertThatThrownBy(() ->
                service.actualizarPerfilImpacto(UUID.randomUUID(), dto(UUID.randomUUID())))
                .isInstanceOf(IllegalStateException.class);

        verify(template, times(1)).execute(any());
    }

    @Test
    @DisplayName("el handler mapea la carrera a 409, que es el código reintentable por definición")
    void elHandlerMapeaAConglicto() throws Exception {
        Method handler = ar.edu.utn.frba.ddsi.incentivos.exceptions.GlobalExceptionHandler.class
                .getMethod("manejarConcurrencia", OptimisticLockingFailureException.class);

        var anotacion = handler.getAnnotation(
                org.springframework.web.bind.annotation.ExceptionHandler.class);

        assertThat(anotacion).isNotNull();
        assertThat(anotacion.value())
                .as("un 500 haría que el cliente trate el fallo como irrecuperable")
                .contains(OptimisticLockingFailureException.class);
    }
}