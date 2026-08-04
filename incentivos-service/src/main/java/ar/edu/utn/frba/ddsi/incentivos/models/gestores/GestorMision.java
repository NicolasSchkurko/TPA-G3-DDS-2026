package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.OperacionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.TipoOperacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioMisiones;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
                AtributoImpacto.ESTADO,
                operacionFactory.coincidencias(1,
                        "ENTREGADA")
                )
            );

        repositorio.agregarMision( misionFactory.crearMision(
                        "Completitud",
                "Usuario Variado",
                        null,
                        AtributoImpacto.CATEGORIA,
                        operacionFactory.valoresDistintos(5,
                                3)
                )
        );

        repositorio.agregarMision( misionFactory.crearMision(
                        "Habil Donador",
                "Usuario Generoso",
                        null,
                        AtributoImpacto.CANTIDAD_BIENES,
                        operacionFactory.superaCantidad(1,
                                3)
                )
        );

        repositorio.agregarMision( misionFactory.crearMision(
                        "Donaciones Exitosas",
                "Usuario Exitoso",
                        null,
                        AtributoImpacto.ESTADO,
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

    public Mision crearMision(String nomMision, String nomInsignia,
                              Integer cantidadTiempo, String unidadTiempo,
                              String atributo, String tipoOperacion,
                              Integer progresoObjetivo, Integer cantidad, String valor){
        ReglaConstancia constancia = new ReglaConstancia(cantidadTiempo,
                ChronoUnit.valueOf(unidadTiempo.trim().toUpperCase(Locale.ROOT)));

        AtributoImpacto atributoImpacto = AtributoImpacto.valueOf(atributo.trim().toUpperCase(Locale.ROOT));

        TipoOperacion tipo = TipoOperacion.valueOf(tipoOperacion.trim().toUpperCase(Locale.ROOT));

        Operacion operacion = conseguirOperacion(tipo, progresoObjetivo, cantidad, valor);

        Mision mision = misionFactory.crearMision(
                nomMision, nomInsignia,
                constancia, atributoImpacto,
                operacion
        );
        repositorio.agregarMision(mision);

        return mision;
    }

    private Operacion conseguirOperacion(TipoOperacion tipo, Integer progresoObjetivo,
                                        Integer cantidad, String valor){
        return switch (tipo) {
            case COINCIDENCIAS ->
                    operacionFactory.coincidencias(progresoObjetivo, valor);
            case SUPERA_CANTIDAD ->
                    operacionFactory.superaCantidad(progresoObjetivo, cantidad);
            case VALORES_DISTINTOS ->
                    operacionFactory.valoresDistintos(progresoObjetivo, cantidad);
        };
    }

    public Mision eliminarMision(UUID idMision) {
        Mision m = repositorio.buscarPorId(idMision);
        repositorio.eliminarMision(m);

        //para retornar al admin
        return m;
    }
}
