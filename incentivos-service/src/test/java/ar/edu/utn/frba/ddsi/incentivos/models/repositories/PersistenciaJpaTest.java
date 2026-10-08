package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.UnidadTiempo;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.PublicacionPendienteN8n;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.ValidadorAdmin;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPublicacionesPendientes;
import ar.edu.utn.frba.ddsi.incentivos.dto.n8n.PerfilPublicacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.PublicacionesN8nService;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:incentivos;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ValidadorAdmin.class, PublicacionesN8nService.class})
class PersistenciaJpaTest {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ValidadorAdmin validadorAdmin;

    @Autowired
    private RepositorioPublicacionesPendientes publicacionesPendientes;

    @Autowired
    private PublicacionesN8nService publicacionesN8nService;

    @MockBean
    private DonacionClient donacionClient;

    @Test
    void persisteLaUnidadConElIdentificadorHistoricoDeLaBase() {
        ReglaConstancia constancia = new ReglaConstancia(3, UnidadTiempo.MONTHS);
        entityManager.persist(constancia);
        entityManager.flush();
        UUID id = constancia.getId();

        entityManager.clear();

        assertThat(jdbcTemplate.queryForObject(
            "SELECT unidad_tiempo FROM regla_constancia WHERE id = ?",
            String.class,
            id
        )).isEqualTo("MONTHS");
        assertThat(entityManager.find(ReglaConstancia.class, id).getUnidadTiempo())
            .isEqualTo(UnidadTiempo.MONTHS);
    }

    @Test
    void reemplazarUnaReglaEliminaLaReglaAnteriorYSusDependencias() {
        Mision mision = crearMision(AtributoImpacto.ESTADO);
        entityManager.persist(mision);
        entityManager.flush();

        UUID idReglaAnterior = mision.getReglaDeProgreso().getIdRegla();
        UUID idConstanciaAnterior = mision.getReglaDeProgreso().getConstancia().getId();
        UUID idOperacionAnterior = mision.getReglaDeProgreso().getOperacion().getIdOperacion();

        mision.actualizar(crearMision(AtributoImpacto.CANTIDAD_BIENES));
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Regla.class, idReglaAnterior)).isNull();
        assertThat(entityManager.find(ReglaConstancia.class, idConstanciaAnterior)).isNull();
        assertThat(entityManager.find(Operacion.class, idOperacionAnterior)).isNull();
    }

    @Test
    void validaElAdminFueraDeLaTransaccionDeEscritura() {
        UUID idAdmin = UUID.randomUUID();
        AtomicBoolean transaccionActivaDuranteLaLlamada = new AtomicBoolean(true);
        TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
        transaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        when(donacionClient.verificarAdmin(idAdmin)).thenAnswer(invocacion -> {
            transaccionActivaDuranteLaLlamada.set(
                TransactionSynchronizationManager.isActualTransactionActive()
            );
            return true;
        });

        transaccion.executeWithoutResult(status -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();

            validadorAdmin.verificarPermisos(idAdmin);

            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
        });

        assertThat(transaccionActivaDuranteLaLlamada.get()).isFalse();
    }

    @Test
    void persisteLaPublicacionPendienteYLaRecuperaTrasLimpiarElContexto() {
        PerfilPublicacionDTO contenido = new PerfilPublicacionDTO(
                "insignia", "mensaje", "discord", "Ana", UUID.randomUUID());
        PublicacionPendienteN8n pendiente =
                publicacionesPendientes.save(new PublicacionPendienteN8n(
                        contenido, LocalDateTime.now()));
        UUID id = pendiente.getId();
        entityManager.flush();
        entityManager.clear();

        PublicacionPendienteN8n recuperada =
                publicacionesPendientes.findById(id).orElseThrow();

        assertThat(recuperada.comoDto().getPrompt()).isEqualTo("insignia");
        assertThat(recuperada.comoDto().getMensaje()).isEqualTo("mensaje");
        assertThat(recuperada.getIntentos()).isZero();
    }

    @Test
    void leaseImpideReclamosDuplicadosYPermiteReintentarLuegoDelPlazo() {
        LocalDateTime ahora = LocalDateTime.now().plusSeconds(1);
        PerfilPublicacionDTO contenido = new PerfilPublicacionDTO(
                "insignia", "mensaje", "discord", "Ana", UUID.randomUUID());
        publicacionesN8nService.encolar(contenido);

        var reclamada = publicacionesN8nService.reclamarSiguiente(ahora).orElseThrow();

        assertThat(publicacionesN8nService.reclamarSiguiente(ahora)).isEmpty();
        publicacionesN8nService.reintentar(reclamada.id(), ahora, "fallo");
        assertThat(publicacionesN8nService.reclamarSiguiente(ahora)).isEmpty();
        assertThat(publicacionesN8nService.reclamarSiguiente(ahora.plusSeconds(31)))
                .isPresent();
    }

    @Test
    void reclamarSiguienteConVariasPublicacionesVencidasDevuelveSoloUna() {
        publicacionesN8nService.encolar(new PerfilPublicacionDTO(
                "insignia", "mensaje", "discord", "Ana", UUID.randomUUID()));
        publicacionesN8nService.encolar(new PerfilPublicacionDTO(
                "insignia", "mensaje", "discord", "Beto", UUID.randomUUID()));
        LocalDateTime ahora = LocalDateTime.now().plusSeconds(1);

        assertThat(publicacionesN8nService.reclamarSiguiente(ahora)).isPresent();
    }

    private Mision crearMision(AtributoImpacto atributo) {
        Regla regla = new Regla(
            new ReglaConstancia(3, UnidadTiempo.MONTHS),
            atributo,
            new SuperaCantidad(2, 1)
        );
        return new Mision("Misión", UUID.randomUUID(), "Descripción", "Insignia", regla);
    }
}
