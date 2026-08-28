package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioMisiones;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @param repositorio gestiona las misiones existentes en el repositorio
 */
@Service
public record GestorMision(RepositorioMisiones repositorio,
                           MisionFactory misionFactory) {

    //init default, dsp el admin puede modificarlas
    public void inicializarMisionesBase() {
        repositorio.agregarMision( misionFactory.crearMision(
                "Racha",
                "realizar 1 donación durante 3 meses consecutivos",
                "Usuario Constante",
                misionFactory.crearConstancia(3, "months"),
                misionFactory.crearAtributoImpacto("estado"),
                misionFactory.crearOperacion("COINCIDENCIAS",1, null,
                        "ENTREGADA")
                )
            );

        repositorio.agregarMision( misionFactory.crearMision(
                        "Completitud",
                "realizar 5 donaciones de 3 categorías distintas",
                "Usuario Variado",
                        null,
                misionFactory.crearAtributoImpacto("categoria"),
                misionFactory.crearOperacion("VALORES_DISTINTOS",5, 3,
                        null)
                )
        );

        repositorio.agregarMision( misionFactory.crearMision(
                        "Habil Donador",
                "1 donación que supere 3 cantidad de bienes",
                "Usuario Generoso",
                        null,
                misionFactory.crearAtributoImpacto("CANTIDAD_BIENES"),
                misionFactory.crearOperacion("SUPERA_CANTIDAD",1, 3,
                        null)
                )
        );

        repositorio.agregarMision( misionFactory.crearMision(
                        "Donaciones Exitosas",
                "Lograr 3 donaciones que sean recibidas exitosamente por una entidad beneficiaria.",
                "Usuario Exitoso",
                        null,
                misionFactory.crearAtributoImpacto("ESTADO"),
                misionFactory.crearOperacion("COINCIDENCIAS",3, null,
                        "ENTREGADA")
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

    public Operacion conseguirOperacion(String tipoOperacion,
                                        Integer progresoObjetivo,
                                        Integer cantidad, String valor){
        return misionFactory.crearOperacion(tipoOperacion, progresoObjetivo, cantidad, valor);
    }

    public ReglaConstancia conseguirConstancia(Integer cantidadTiempo, String unidadTiempo){
        return misionFactory.crearConstancia(cantidadTiempo, unidadTiempo);
    }

    public Mision crearMision(String nomMision, String descripcion, String nomInsignia,
                              ReglaConstancia constancia, String atributo,
                              Operacion operacion){
        AtributoImpacto atributoImpacto = misionFactory.crearAtributoImpacto(atributo);

        Mision mision = misionFactory.crearMision(
                nomMision, descripcion, nomInsignia,
                constancia,
                atributoImpacto,
                operacion
        );
        repositorio.agregarMision(mision);

        return mision;
    }

    public Mision eliminarMision(UUID idMision) {
        Mision m = repositorio.buscarPorId(idMision);
        repositorio.eliminarMision(m);

        //para retornar al admin
        return m;
    }

    public Mision actualizarMision(Mision mision) {
        if(mision.getIdMision() == null) return null;

        Mision misionActual = repositorio.buscarPorId(mision.getIdMision());
        if(misionActual == null) return null;

        if(mision.getNombreMision() != null){
            misionActual.setNombreMision(mision.getNombreMision());
            misionActual.getInsigniaObjetivo().setDescripcion(mision.getNombreMision());
        }

        if(mision.getInsigniaObjetivo() != null){
            misionActual.setInsigniaObjetivo(
                    new Insignia(
                            mision.getInsigniaObjetivo().getNombre(),
                            misionActual.getNombreMision()
                    )
            );
        }

        //no esta permitido reescribir la reglaProgreso en una mision
        //muy complejo y quiza no tiene sentido si podes crear una regla nueva
        //y eliminar la anterior o mantenerla

        return repositorio.actualizar(misionActual);
    }
}
