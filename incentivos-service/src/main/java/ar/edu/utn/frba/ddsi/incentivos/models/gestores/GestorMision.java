package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.OperacionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioMisiones;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @param repositorio gestiona las misiones existentes en el repositorio
 */
public record GestorMision(RepositorioMisiones repositorio,
                           MisionFactory misionFactory,
                           OperacionFactory operacionFactory) {

    //init default, dsp el admin puede modificarlas
    public void inicializarMisionesBase() {
        repositorio.agregarMision( misionFactory.crearMision(
                "Racha",
                "Usuario Constante",
                new ReglaConstancia(1, ChronoUnit.MONTHS),
                ImpactoDonacion::getEstado,
                operacionFactory.coincidencias(1,
                        "ENTREGADA")
                )
            );

        repositorio.agregarMision( misionFactory.crearMision(
                        "Completitud",
                "Usuario Variado",
                        null,
                        ImpactoDonacion::getCategoria,
                        operacionFactory.valoresDistintos(5,
                                3)
                )
        );

        repositorio.agregarMision( misionFactory.crearMision(
                        "Habil Donador",
                "Usuario Generoso",
                        null,
                        ImpactoDonacion::getCantidadBienes,
                        operacionFactory.superaCantidad(1,
                                3)
                )
        );

        repositorio.agregarMision( misionFactory.crearMision(
                        "Donaciones Exitosas",
                "Usuario Exitoso",
                        null,
                        ImpactoDonacion::getEstado,
                        operacionFactory.coincidencias(3,
                                "ENTREGADO")
                )
        );
    }

    public List<Mision> conseguirMisiones(List<UUID> idMisiones) {
        List<Mision> misiones = new ArrayList<>();

        for (UUID idMision : idMisiones) {
            List<Mision> lstMisiones = repositorio.obtenerTodas();
            for (Mision m : lstMisiones) {
                if (m.getIdMision().equals(idMision)) {
                    misiones.add(m);
                }
            }
        }

        return misiones;
    }


}
