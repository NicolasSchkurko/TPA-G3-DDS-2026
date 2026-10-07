package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * La pasada de constancia tiene que consultar los perfiles dentro de la transacción del bloque:
 * fuera de ella las entidades llegan desligadas y tocar `valoresObservados` (LAZY) lanza
 * LazyInitializationException (punto 6).
 */
@DisplayName("Punto 6: la consulta de constancia corre dentro de la transacción del bloque")
class PerfilServiceConstanciaTransaccionalTest {

    @Test
    void laConsultaDePerfilesCorreDentroDeLaTransaccionDelBloque() {
        RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
        RepositorioDonaciones repoDonaciones = mock(RepositorioDonaciones.class);
        AtomicBoolean enTransaccion = new AtomicBoolean(false);
        AtomicBoolean consultadaDentroDeLaTransaccion = new AtomicBoolean(false);

        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any())).thenAnswer(invocacion -> {
            enTransaccion.set(true);
            Object resultado = ((TransactionCallback<?>) invocacion.getArgument(0))
                    .doInTransaction(mock(TransactionStatus.class));
            enTransaccion.set(false);
            return resultado;
        });

        when(repoPerfiles.buscarPerfilesConMisionQueRequiereConstancia(any()))
                .thenAnswer(invocacion -> {
                    consultadaDentroDeLaTransaccion.set(enTransaccion.get());
                    return new PageImpl<>(List.of(perfilConMisionDeConstancia()));
                });

        new PerfilService(repoPerfiles,
                mock(RepositorioCategorias.class),
                repoDonaciones,
                transactionTemplate)
                .evaluarConstanciaPerfiles();

        assertThat(consultadaDentroDeLaTransaccion.get()).isTrue();
    }

    private Perfil perfilConMisionDeConstancia() {
        Mision mision = mock(Mision.class);
        when(mision.getIdMision()).thenReturn(UUID.randomUUID());

        ProgresoMision progreso = mock(ProgresoMision.class);
        when(progreso.getMision()).thenReturn(mision);

        Perfil perfil = mock(Perfil.class);
        when(perfil.getIdUsuario()).thenReturn(UUID.randomUUID());
        when(perfil.getProgresoMisionActual()).thenReturn(progreso);

        return perfil;
    }
}
