package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.ActividadMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.MetricasActividad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.TipoCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.*;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;

import java.time.YearMonth;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioRankings;
import org.springframework.stereotype.Service;

@Service
public class PerfilService {
    private final RepositorioDonaciones repositorioDonaciones = RepositorioDonaciones.getInstance();
    private final RepositorioPerfiles repositorioPerfiles = RepositorioPerfiles.getInstance();
    private final RepositorioRankings repositorioRankings = RepositorioRankings.getInstance();
    private final RepositorioCategorias repositorioCategorias = RepositorioCategorias.getInstance();

    public MetricasActividad obtenerMetricasDonante(UUID idUsuario){
        List<ImpactoDonacion> historial = repositorioDonaciones.buscarDonacionesPorIDUsuario(idUsuario);

        YearMonth mesActual = YearMonth.now();
        YearMonth mesAnterior = mesActual.minusMonths(1);

        ActividadMensual actividadActual = new ActividadMensual(mesActual, historial);
        ActividadMensual actividadAnterior = new ActividadMensual(mesAnterior, historial);

        return new MetricasActividad(actividadActual, actividadAnterior);
    }

    public List<ActividadMensual> obtenerEvolucionHistorica(UUID idUsuario){
        List<ImpactoDonacion> historial = repositorioDonaciones.buscarDonacionesPorIDUsuario(idUsuario);

        return historial.stream()
                .filter(d -> d.getFechaEntrega() != null && "ENTREGADA".equalsIgnoreCase(d.getEstado()))
                .map(d -> YearMonth.from(d.getFechaEntrega()))
                .distinct()
                .sorted() // Ordenados cronológicamente
                .map(periodo -> new ActividadMensual(periodo, historial))
                .toList();
    }

    public RankingMensual generarRankingMensual(YearMonth periodo) {
        List<Perfil> todosLosPerfiles = repositorioPerfiles.listarTodos();

        AtomicInteger puestoCounter = new AtomicInteger(1);

        // Local candidate class to hold count and last completion date for tie-breaking
        class Candidate {
            Perfil perfil;
            int misiones;
            LocalDate ultimoCompletado;

            Candidate(Perfil perfil, int misiones, LocalDate ultimoCompletado) {
                this.perfil = perfil;
                this.misiones = misiones;
                this.ultimoCompletado = ultimoCompletado;
            }
        }

        // 1. Construimos candidatos con cantidad y fecha de última misión en el periodo
        List<PosicionRanking> posicionesFinales = todosLosPerfiles.stream()
                .map(perfil -> {
                    int misionesEnPeriodo = (int) perfil.getInsignias().stream()
                            .filter(insignia -> insignia.getFechaObtencion() != null &&
                                    YearMonth.from(insignia.getFechaObtencion()).equals(periodo))
                            .count();

                    LocalDate ultimoCompletado = perfil.getInsignias().stream()
                            .filter(insignia -> insignia.getFechaObtencion() != null &&
                                    YearMonth.from(insignia.getFechaObtencion()).equals(periodo))
                            .map(Insignia::getFechaObtencion)
                            .max(LocalDate::compareTo)
                            .orElse(null);

                    return new Candidate(perfil, misionesEnPeriodo, ultimoCompletado);
                })
                .filter(c -> c.misiones > 0)
                // Ordenamos por misiones desc; en empate, gana quien completó antes (ultimoCompletado más temprano)
                .sorted((c1, c2) -> {
                    int cmp = Integer.compare(c2.misiones, c1.misiones);
                    if (cmp != 0) return cmp;

                    if (c1.ultimoCompletado == null && c2.ultimoCompletado == null) return 0;
                    if (c1.ultimoCompletado == null) return 1; // nulls go last
                    if (c2.ultimoCompletado == null) return -1;

                    return c1.ultimoCompletado.compareTo(c2.ultimoCompletado); // earlier date -> higher rank
                })
                // Mapeamos a PosicionRanking y asignamos puesto incremental
                .map(c -> {
                    PosicionRanking p = new PosicionRanking(null, c.perfil.getIdPerfil(), c.perfil.getIdUsuario(),
                            c.perfil.getNombreUsuario(), c.misiones);
                    p.setPuesto(puestoCounter.getAndIncrement());
                    return p;
                })
                .toList();

        // 2. Construimos y persistimos el objeto de dominio del ranking
        RankingMensual rankingDelMes = new RankingMensual(periodo, posicionesFinales);
        repositorioRankings.guardar(rankingDelMes);

        return rankingDelMes;
    }

    public List<PosicionRanking> obtenerTop3DelMes(YearMonth periodo) {
        RankingMensual ranking = repositorioRankings.buscarPorPeriodo(periodo);
        if (ranking == null || ranking.getPosiciones() == null) {
            return List.of();
        }

        return ranking.getPosiciones().stream()
                .limit(3) // Nos quedamos solo con los 3 primeros elementos de la lista ya ordenada
                .toList();
    }

    public Mision obtenerMisionPorID(UUID idUsuario) {
        Perfil perfil = repositorioPerfiles.buscarPorIDUsuario(idUsuario);

        return perfil.getMisionActual();
    }

    public void progresarMisionesPerfil(Perfil perfil, ImpactoDonacion donacion) {
        Mision misionActual = perfil.getMisionActual();
        if (misionActual == null) return;

        misionActual.evaluarDonacion(donacion);

        // Si se completó, le otorgamos su insignia
        if (misionActual.estaCompleta()) {
            perfil.otorgarInsignia(misionActual.getInsigniaObjetivo());
            perfil.sumarMisionCumplida();

            // Buscamos la estructura de la categoría actual usando nuestro Repositorio dinámico
            Categoria categoriaObj = repositorioCategorias.buscarPorTipo(perfil.getCategoriaActual());

            if (categoriaObj != null) {
                // Caso A: Era la última misión de la categoría, sube de nivel
                if (categoriaObj.esUltimaMision(misionActual)) {
                    TipoCategoria siguienteNivel = categoriaObj.getSiguienteCategoria();
                    perfil.setCategoriaActual(siguienteNivel);

                    // Le asignamos la primera misión del nuevo rango
                    Categoria nuevaCategoria = repositorioCategorias.buscarPorTipo(siguienteNivel);
                    perfil.setMisionActual(nuevaCategoria != null ? nuevaCategoria.primeraMision() : null);
                }
                // Caso B: Quedan misiones en esta categoría, avanzamos a la siguiente
                else {
                    perfil.setMisionActual(categoriaObj.siguienteMision(misionActual));
                }
            }
        }
    }

    public MetricasActividadDTO convertirMetricaADTO(MetricasActividad metrica){
        return new MetricasActividadDTO(metrica.getPeriodo(),
                metrica.getVariacionPorcentualDonaciones(),
                metrica.getVariacionPorcentualOrganizaciones());
    }

    public ActividadMensualDTO convertirActividadADTO(ActividadMensual actividad){
        return new ActividadMensualDTO( actividad.getPeriodo(),
                actividad.getCantidadDonaciones(),
                actividad.getOrganizacionesAyudadas());
    }

    public MisionDTO convertirMisionADTO(Mision mision) {
        return new MisionDTO(mision.getNombreMision(),
                             mision.getProgresoActual(),
                             mision.getProgresoObjetivo());
    }

    public List<Insignia> obtenerInsigniasPorID(UUID idUsuario) {
        Perfil perfil = repositorioPerfiles.buscarPorIDUsuario(idUsuario);

        return perfil.getInsignias();
    }

    public InsigniaDTO convertirInsigniaADTO(Insignia insignia) {
        return new InsigniaDTO( insignia.getNombre(),
                                insignia.getDescripcion(),
                                insignia.getUrlImagen(),
                                insignia.getFechaObtencion());
    }
}