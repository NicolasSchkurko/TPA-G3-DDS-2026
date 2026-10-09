package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.ValidadorAdmin;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@DisplayName("Punto 6: constancia consultada y recalculada en la misma transacción")
class PerfilServiceConstanciaTransaccionalTest {

    @Test
    @DisplayName("los perfiles se buscan dentro de la transacción que los recalcula")
    void laConsultaCorreDentroDeLaTransaccion() {
        RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
        RepositorioDonaciones repoDonaciones = mock(RepositorioDonaciones.class);

        AtomicBoolean enTransaccion = new AtomicBoolean(false);
        AtomicBoolean consultadaDentroDeLaTransaccion = new AtomicBoolean(false);

        // Un template que corre el callback de verdad y enciende la bandera mientras corre.
        TransactionTemplate template = mock(TransactionTemplate.class);
        when(template.execute(any())).thenAnswer(invocacion -> {
            TransactionCallback<?> accion = invocacion.getArgument(0);
            enTransaccion.set(true);
            try {
                return accion.doInTransaction(null);
            } finally {
                enTransaccion.set(false);
            }
        });

        // La consulta captura si la bandera estaba encendida en el momento en que se pidió.
        Perfil perfil = mock(Perfil.class);
        ProgresoMision progreso = mock(ProgresoMision.class);
        Mision mision = mock(Mision.class);
        UUID idUsuario = UUID.randomUUID();
        UUID idMision = UUID.randomUUID();

        when(perfil.getProgresoMisionActual()).thenReturn(progreso);
        when(progreso.getMision()).thenReturn(mision);
        when(mision.getIdMision()).thenReturn(idMision);
        when(perfil.getIdUsuario()).thenReturn(idUsuario);
        when(repoDonaciones.findByIdUsuarioAndIdMisionOrderByFechaEntregaAsc(idUsuario, idMision))
                .thenReturn(List.of());

        when(repoPerfiles.buscarPerfilesConMisionQueRequiereConstancia(any(Pageable.class)))
                .thenAnswer(invocacion -> {
                    consultadaDentroDeLaTransaccion.set(enTransaccion.get());
                    // Menos de TAMANO_BLOQUE_CONSTANCIA para que el loop termine en la vuelta.
                    return new PageImpl<>(List.of(perfil));
                });

        PerfilService service = new PerfilService(
                repoPerfiles,
                mock(RepositorioCategorias.class),
                repoDonaciones,
                template,
                mock(ValidadorAdmin.class));

        service.evaluarConstanciaPerfiles();

        assertThat(consultadaDentroDeLaTransaccion).isTrue();
    }
}
