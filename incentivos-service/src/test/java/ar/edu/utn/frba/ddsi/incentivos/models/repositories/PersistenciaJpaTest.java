package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.UnidadTiempo;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.InsigniaObtenida;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.PublicacionPendienteN8n;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.SincronizacionPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPublicacionesPendientes;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioRankings;
import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.PerfilNotificacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.ActividadDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.MetricaDonacionesDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingFilaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingMesDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.n8n.PerfilPublicacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.NotificacionesPendientesService;
import ar.edu.utn.frba.ddsi.incentivos.services.MetricasService;
import ar.edu.utn.frba.ddsi.incentivos.services.RankingService;
import ar.edu.utn.frba.ddsi.incentivos.services.PublicacionesN8nService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.time.YearMonth;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
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
@Import({PublicacionesN8nService.class, NotificacionesPendientesService.class, MetricasService.class, RankingService.class, SincronizacionPerfiles.class})
class PersistenciaJpaTest {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RepositorioPublicacionesPendientes publicacionesPendientes;

    @Autowired
    private PublicacionesN8nService publicacionesN8nService;

    @Autowired
    private NotificacionesPendientesService notificacionesPendientesService;

    @Autowired
    private RepositorioRankings repositorioRankings;

    @Autowired
    private RepositorioPerfiles repositorioPerfiles;

    @Autowired
    private RepositorioDonaciones repositorioDonaciones;

    @Autowired
    private MetricasService metricasService;

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
    void persisteLaPublicacionPendienteYLaRecuperaTrasLimpiarElContexto() {
        PerfilPublicacionDTO contenido = new PerfilPublicacionDTO(
                "insignia", "mensaje", "Ana", UUID.randomUUID());
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
    void actividadDeUnDonanteConDonacionesDevuelveLosTotales() {
        UUID idUsuario = UUID.randomUUID();
        entityManager.persist(new Perfil(idUsuario, "Ana"));
        persistirImpacto(idUsuario, LocalDateTime.of(2026, 10, 1, 10, 0), 3, "Banco de alimentos");
        persistirImpacto(idUsuario, LocalDateTime.of(2026, 10, 2, 11, 0), 5, "Banco de alimentos");
        persistirImpacto(idUsuario, LocalDateTime.of(2026, 11, 3, 9, 0), 1, "Comedor");
        entityManager.flush();
        entityManager.clear();

        ActividadDTO actividad = metricasService.obtenerEvolucionHistorica(idUsuario);

        assertThat(actividad.getRegistros()).hasSize(2);
        assertThat(actividad.getTotalDonaciones()).isEqualTo(3);
        assertThat(actividad.getTotalOrganizaciones()).isEqualTo(2);
    }

    @Test
    void actividadDeUnDonanteSinDonacionesDevuelveTotalesEnCero() {
        UUID idUsuario = UUID.randomUUID();
        entityManager.persist(new Perfil(idUsuario, "Beto"));
        entityManager.flush();
        entityManager.clear();

        ActividadDTO actividad = metricasService.obtenerEvolucionHistorica(idUsuario);

        assertThat(actividad.getRegistros()).isEmpty();
        assertThat(actividad.getTotalDonaciones()).isZero();
        assertThat(actividad.getTotalOrganizaciones()).isZero();
    }

    @Test
    void metricaDeUnPeriodoConVariasDonacionesDevuelveElResumen() {
        UUID idUsuario = UUID.randomUUID();
        persistirImpacto(idUsuario, LocalDateTime.of(2026, 10, 1, 10, 0), 3, "Banco de alimentos");
        persistirImpacto(idUsuario, LocalDateTime.of(2026, 10, 2, 11, 0), 5, "Banco de alimentos");
        entityManager.flush();
        entityManager.clear();

        MetricaDonacionesDTO dto = metricasService
                .obtenerMetrica(idUsuario, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
                .orElseThrow();

        assertThat(dto.getCantidadDonaciones()).isEqualTo(2);
        assertThat(dto.getCantidadBienes()).isEqualTo(8);
        assertThat(dto.getEntidadesBeneficiarias()).containsExactly("Banco de alimentos");
    }

    @Test
    void metricaDeUnDiaQueTieneDonacionesDevuelveElResumen() {
        UUID idUsuario = UUID.randomUUID();
        persistirImpacto(idUsuario, LocalDateTime.of(2026, 10, 7, 10, 0), 2, "Comedor");
        persistirImpacto(idUsuario, LocalDateTime.of(2026, 10, 7, 15, 0), 1, "Comedor");
        entityManager.flush();
        entityManager.clear();

        MetricaDonacionesDTO dto = metricasService
                .obtenerMetrica(idUsuario, LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 7))
                .orElseThrow();

        assertThat(dto.getCantidadDonaciones()).isEqualTo(2);
        assertThat(dto.getCantidadBienes()).isEqualTo(3);
    }

    @Test
    void unaDonacionJustoALasCeroDelDiaSiguienteEntraEnElPeriodo() {
        UUID idUsuario = UUID.randomUUID();
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 7);
        persistirImpacto(idUsuario, hasta.plusDays(1).atStartOfDay(), 9, "Comedor");
        persistirImpacto(idUsuario, hasta.atTime(23, 59, 59), 2, "Comedor");
        entityManager.flush();
        entityManager.clear();

        MetricaDonacionesDTO dto = metricasService
                .obtenerMetrica(idUsuario, desde, hasta)
                .orElseThrow();

        assertThat(dto.getCantidadDonaciones()).isEqualTo(2);
        assertThat(dto.getCantidadBienes()).isEqualTo(11);
        assertThat(dto.getEntidadesBeneficiarias()).containsExactly("Comedor");
    }

    @Test
    void metricaDeUnPeriodoSinDonacionesEsVacia() {
        UUID idUsuario = UUID.randomUUID();

        assertThat(metricasService.obtenerMetrica(
                idUsuario, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .isEmpty();
    }

    private void persistirImpacto(UUID idUsuario, LocalDateTime fecha,
                                  int cantidadBienes, String entidad) {
        entityManager.persist(new ImpactoDonacion(
                UUID.randomUUID(), idUsuario, entidad, cantidadBienes, fecha,
                "Indumentaria", "Ropa", "ENTREGADA"));
    }

    @Test
    void destacadasFiltraEntregadasConMisionYOrdenaDescendente() {
        LocalDateTime base = LocalDateTime.of(2026, 5, 1, 10, 0);
        ImpactoDonacion vieja = guardarImpacto(base, "ENTREGADA", true);
        ImpactoDonacion nueva = guardarImpacto(base.plusDays(1), "ENTREGADA", true);
        guardarImpacto(base.plusDays(2), "ENTREGADO", true);
        guardarImpacto(base.plusDays(3), "ENTREGADA", false);
        guardarImpacto(null, "ENTREGADA", true);
        entityManager.flush();
        entityManager.clear();

        List<ImpactoDonacion> resultado =
                repositorioDonaciones.buscarDestacadas(PageRequest.of(0, 10));

        assertThat(resultado)
                .extracting(ImpactoDonacion::getIdDonacion)
                .containsExactly(nueva.getIdDonacion(), vieja.getIdDonacion());
    }

    @Test
    void destacadasRespetaElLimitePedido() {
        LocalDateTime base = LocalDateTime.of(2026, 5, 1, 10, 0);
        guardarImpacto(base, "ENTREGADA", true);
        guardarImpacto(base.plusDays(1), "ENTREGADA", true);
        guardarImpacto(base.plusDays(2), "ENTREGADA", true);
        entityManager.flush();

        assertThat(repositorioDonaciones.buscarDestacadas(PageRequest.of(0, 2)))
                .hasSize(2);
    }

    @Test
    void destacadasDevuelveListaVaciaSiNoHayCoincidencias() {
        assertThat(repositorioDonaciones.buscarDestacadas(PageRequest.of(0, 10)))
                .isEmpty();
    }

    private ImpactoDonacion guardarImpacto(
            LocalDateTime fecha, String estado, boolean completoMision) {
        ImpactoDonacion donacion = new ImpactoDonacion(
                UUID.randomUUID(), UUID.randomUUID(), "Comedor", 1,
                fecha, "ALIMENTOS", "MERCEARIA", estado);
        donacion.registrarSiCompletoMision(completoMision);
        return repositorioDonaciones.save(donacion);
    }

    @Test
    void leaseImpideReclamosDuplicadosYPermiteReintentarLuegoDelPlazo() {
        LocalDateTime ahora = LocalDateTime.now().plusSeconds(1);
        PerfilPublicacionDTO contenido = new PerfilPublicacionDTO(
                "insignia", "mensaje", "Ana", UUID.randomUUID());
        publicacionesN8nService.encolar(contenido);

        var reclamada = publicacionesN8nService.reclamarSiguiente(ahora).orElseThrow();

        assertThat(publicacionesN8nService.reclamarSiguiente(ahora)).isEmpty();
        publicacionesN8nService.reintentar(reclamada.id(), ahora, "fallo");
        assertThat(publicacionesN8nService.reclamarSiguiente(ahora)).isEmpty();
        assertThat(publicacionesN8nService.reclamarSiguiente(ahora.plusSeconds(31)))
                .isPresent();
    }

    @Test
    void unaPublicacionDescartadaNoSeReclama() {
        LocalDateTime ahora = LocalDateTime.now().plusSeconds(1);
        PerfilPublicacionDTO contenido = new PerfilPublicacionDTO(
                "insignia", "mensaje", "Ana", UUID.randomUUID());
        publicacionesN8nService.encolar(contenido);

        for (int i = 0; i < 10; i++) {
            var reclamada = publicacionesN8nService.reclamarSiguiente(ahora).orElseThrow();
            publicacionesN8nService.reintentar(reclamada.id(), ahora, "fallo");
            ahora = ahora.plusDays(2);
        }

        assertThat(publicacionesN8nService.reclamarSiguiente(ahora.plusDays(2))).isEmpty();
    }

    @Test
    void reclamarSiguienteConVariasPublicacionesVencidasDevuelveSoloUna() {
        publicacionesN8nService.encolar(new PerfilPublicacionDTO(
                "insignia", "mensaje", "Ana", UUID.randomUUID()));
        publicacionesN8nService.encolar(new PerfilPublicacionDTO(
                "insignia", "mensaje", "Beto", UUID.randomUUID()));
        LocalDateTime ahora = LocalDateTime.now().plusSeconds(1);

        assertThat(publicacionesN8nService.reclamarSiguiente(ahora)).isPresent();
    }

    @Test
    void historialRankingsPaginaCorrectamenteEnBaseDeDatos() {
        RankingMensual r1 = new RankingMensual(YearMonth.of(2026, 1));
        r1.agregarPosicion(new Ranking(r1, UUID.randomUUID(), "usuario1", 1, 10L));
        entityManager.persist(r1);

        RankingMensual r2 = new RankingMensual(YearMonth.of(2026, 2));
        r2.agregarPosicion(new Ranking(r2, UUID.randomUUID(), "usuario2", 1, 15L));
        entityManager.persist(r2);

        RankingMensual r3 = new RankingMensual(YearMonth.of(2026, 3));
        r3.agregarPosicion(new Ranking(r3, UUID.randomUUID(), "usuario3", 1, 20L));
        entityManager.persist(r3);

        entityManager.flush();
        entityManager.clear();

        Page<RankingMensual> pagina =
            repositorioRankings.findAllByOrderByPeriodoDesc(PageRequest.of(0, 2));

        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getTotalPages()).isEqualTo(2);
        assertThat(pagina.getContent()).hasSize(2);
        assertThat(pagina.getContent().get(0).getPeriodo()).isEqualTo(YearMonth.of(2026, 3));
        assertThat(pagina.getContent().get(1).getPeriodo()).isEqualTo(YearMonth.of(2026, 2));
        assertThat(pagina.getContent().get(0).getPosiciones()).hasSize(1);
    }

    @Test
    void calcularRankingMensualProyectaFilasYPagina() {
        LocalDateTime ahora = LocalDateTime.now();
        for (String nombre : List.of("Ana", "Beto", "Caro")) {
            Perfil perfil = new Perfil(UUID.randomUUID(), nombre);
            Insignia insignia = new Insignia("Medalla " + nombre, "Descripcion");
            entityManager.persist(insignia);
            perfil.getInsigniasObtenidas().add(new InsigniaObtenida(perfil, insignia));
            entityManager.persist(perfil);
        }
        entityManager.flush();
        entityManager.clear();

        Slice<RankingFilaDTO> primera = repositorioPerfiles.calcularRankingMensual(
                ahora.minusDays(1), ahora.plusDays(1), PageRequest.of(0, 2));

        assertThat(primera.getContent())
                .extracting(RankingFilaDTO::nombreUsuario)
                .containsExactly("Ana", "Beto");
        assertThat(primera.getContent())
                .extracting(RankingFilaDTO::totalMisiones)
                .containsOnly(1L);
        assertThat(primera.hasNext()).isTrue();

        Slice<RankingFilaDTO> segunda = repositorioPerfiles.calcularRankingMensual(
                ahora.minusDays(1), ahora.plusDays(1), PageRequest.of(1, 2));

        assertThat(segunda.getContent())
                .extracting(RankingFilaDTO::nombreUsuario)
                .containsExactly("Caro");
        assertThat(segunda.hasNext()).isFalse();
    }

    @Test
    void paginaPosicionesDevuelveSoloLaPaginaPedida() {
        RankingMensual ranking = new RankingMensual(YearMonth.of(2026, 4));
        entityManager.persist(ranking);
        entityManager.flush();
        UUID idRanking = ranking.getIdRanking();

        for (int puesto = 1; puesto <= 3; puesto++) {
            entityManager.persist(new Ranking(
                    ranking, UUID.randomUUID(), "Donante " + puesto, puesto, (long) (4 - puesto)));
        }
        entityManager.flush();
        entityManager.clear();

        Slice<Ranking> primera = repositorioRankings.paginaPosiciones(
                idRanking, PageRequest.of(0, 2));

        assertThat(primera.getContent()).extracting(Ranking::getPuesto).containsExactly(1, 2);
        assertThat(primera.hasNext()).isTrue();

        Slice<Ranking> segunda = repositorioRankings.paginaPosiciones(
                idRanking, PageRequest.of(1, 2));

        assertThat(segunda.getContent()).extracting(Ranking::getPuesto).containsExactly(3);
        assertThat(segunda.hasNext()).isFalse();
    }

    @Test
    void laBaseRechazaDosInsigniasObtenidasDelMismoPerfilEInsignia() {
        Perfil perfil = new Perfil(UUID.randomUUID(), "Ana");
        Insignia insignia = new Insignia("Medalla", "desc");
        entityManager.persist(insignia);
        entityManager.persist(perfil);
        entityManager.persist(new InsigniaObtenida(perfil, insignia));
        entityManager.flush();

        entityManager.persist(new InsigniaObtenida(perfil, insignia));

        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void laOutboxDeNotificacionesPersisteYReclamaConLease() {
        PerfilNotificacionDTO dto = new PerfilNotificacionDTO(
                "EMAIL", "ana@example.com", "Mensaje", "Asunto");
        notificacionesPendientesService.encolar(dto);
        LocalDateTime ahora = LocalDateTime.now().plusSeconds(1);

        var reclamada = notificacionesPendientesService.reclamarSiguiente(ahora).orElseThrow();

        assertThat(reclamada.contenido().getDireccionDeContacto()).isEqualTo("ana@example.com");
        assertThat(notificacionesPendientesService.reclamarSiguiente(ahora)).isEmpty();

        notificacionesPendientesService.reintentar(reclamada.id(), ahora, "fallo");

        assertThat(notificacionesPendientesService.reclamarSiguiente(ahora.plusSeconds(31)))
                .isPresent();
    }

    @Test
    void buscarPerfilesConMisionQueRequiereConstanciaPaginaYFiltra() {
        Mision conConstancia = crearMision(AtributoImpacto.ESTADO);
        Mision sinConstancia = new Mision(
                "Sin constancia", null, "desc", "Insignia sin constancia",
                new Regla(null, AtributoImpacto.ESTADO, new SuperaCantidad(2, 1)));
        entityManager.persist(conConstancia);
        entityManager.persist(sinConstancia);

        Categoria categoriaCon = new Categoria("Con", null, 1, List.of(conConstancia));
        Categoria categoriaSin = new Categoria("Sin", null, 2, List.of(sinConstancia));
        entityManager.persist(categoriaCon);
        entityManager.persist(categoriaSin);

        Perfil perfilCon = new Perfil(UUID.randomUUID(), "Con constancia");
        perfilCon.iniciarEn(categoriaCon);
        entityManager.persist(perfilCon);

        Perfil perfilSin = new Perfil(UUID.randomUUID(), "Sin constancia");
        perfilSin.iniciarEn(categoriaSin);
        entityManager.persist(perfilSin);

        entityManager.flush();
        entityManager.clear();

        Page<Perfil> pagina = repositorioPerfiles
                .buscarPerfilesConMisionQueRequiereConstancia(PageRequest.of(0, 10));

        assertThat(pagina.getContent())
                .extracting(Perfil::getNombreUsuario)
                .containsExactly("Con constancia");
        assertThat(pagina.getContent().get(0).getProgresoMisionActual()
                .getMision().getNombreMision())
                .isEqualTo("Misión");
    }

    @Test
    void buscarDonacionesQueProgresaronDePerfilesHaceUnaSolaConsulta() {
        UUID usuario1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID usuario2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID mision1 = UUID.randomUUID();
        UUID mision2 = UUID.randomUUID();
        UUID mision3 = UUID.randomUUID();
        LocalDateTime base = LocalDateTime.of(2026, 3, 1, 10, 0);

        ImpactoDonacion primera = donacionDe(usuario1, mision1, true, base);
        ImpactoDonacion segunda = donacionDe(usuario1, mision1, true, base.plusDays(1));
        ImpactoDonacion tercera = donacionDe(usuario2, mision2, true, base);
        ImpactoDonacion otraMision = donacionDe(usuario2, mision3, true, base);
        ImpactoDonacion sinProgreso = donacionDe(usuario1, mision1, false, base.plusDays(2));
        for (ImpactoDonacion donacion : List.of(
                primera, segunda, tercera, otraMision, sinProgreso)) {
            entityManager.persist(donacion);
        }
        entityManager.flush();
        entityManager.clear();

        List<ImpactoDonacion> resultado = repositorioDonaciones
                .buscarDonacionesQueProgresaronDePerfiles(
                        List.of(usuario1, usuario2), Set.of(mision1, mision2));

        assertThat(resultado)
                .extracting(ImpactoDonacion::getIdDonacion)
                .containsExactly(primera.getIdDonacion(), segunda.getIdDonacion(),
                        tercera.getIdDonacion());
    }

    private static ImpactoDonacion donacionDe(UUID idUsuario, UUID idMision,
                                               boolean progreso, LocalDateTime fecha) {
        ImpactoDonacion donacion = new ImpactoDonacion(
                UUID.randomUUID(), idUsuario, "Fundacion", 1, fecha,
                "ROPA", "INDUMENTARIA", "ENTREGADA");
        donacion.registrarProgresoEn(idMision, progreso);
        return donacion;
    }

    @Test
    void findByIdParaDonacionNoInicializaLasInsignias() {
        UUID idPerfil = UUID.randomUUID();
        Perfil perfil = new Perfil(idPerfil, "Ana");
        Insignia insignia = new Insignia("Medalla", "desc");
        entityManager.persist(insignia);
        perfil.getInsigniasObtenidas().add(new InsigniaObtenida(perfil, insignia));
        entityManager.persist(perfil);
        entityManager.flush();
        entityManager.clear();

        Perfil recuperado = repositorioPerfiles.findByIdParaDonacion(idPerfil).orElseThrow();

        assertThat(Hibernate.isInitialized(recuperado.getInsigniasObtenidas())).isFalse();
    }

    @Test
    void generarRankingPersisteTodasLasPosicionesPorPaginas() throws Exception {
        LocalDateTime fecha = YearMonth.now().minusMonths(1).atDay(1).atStartOfDay();

        Field fechaObtencion = InsigniaObtenida.class.getDeclaredField("fechaObtencion");
        fechaObtencion.setAccessible(true);
        for (int i = 0; i < 501; i++) {
            Perfil perfil = new Perfil(UUID.randomUUID(), "Donante " + i);
            Insignia insignia = new Insignia("Medalla " + i, "desc");
            entityManager.persist(insignia);
            InsigniaObtenida obtenida = new InsigniaObtenida(perfil, insignia);
            fechaObtencion.set(obtenida, fecha);
            perfil.getInsigniasObtenidas().add(obtenida);
            entityManager.persist(perfil);
        }
        entityManager.flush();
        entityManager.clear();

        RankingService service = new RankingService(
                repositorioRankings, repositorioPerfiles);
        org.springframework.test.util.ReflectionTestUtils.setField(
                service, "entityManager", entityManager);
        RankingMesDTO creado = service.crearRankingMensual(
                UUID.randomUUID(), YearMonth.now().minusMonths(1));

        assertThat(creado.getRanking().getContent())
                .hasSize(RankingService.RANKING_PREDETERMINADO);
        Page<Ranking> posiciones = repositorioRankings.paginaPosiciones(
                creado.getIdRanking(), PageRequest.of(0, 1000));
        assertThat(posiciones.getContent()).hasSize(501);
        assertThat(posiciones.getContent()).allMatch(r -> r.getMisionesCumplidas() == 1L);
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
