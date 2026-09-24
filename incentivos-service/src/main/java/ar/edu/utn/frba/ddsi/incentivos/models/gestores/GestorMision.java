package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioMisiones;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GestorMision {
    private final RepositorioMisiones repositorio;
    private final MisionFactory misionFactory;

    public GestorMision(RepositorioMisiones repositorio, MisionFactory misionFactory) {
        this.repositorio = repositorio;
        this.misionFactory = misionFactory;
    }

    public Operacion conseguirOperacion(String tipoOperacion,
                                        Integer progresoObjetivo,
                                        Integer cantidad, String valor) {
        return misionFactory.crearOperacion(tipoOperacion, progresoObjetivo, cantidad, valor);
    }

    public ReglaConstancia conseguirConstancia(Integer cantidadTiempo, String unidadTiempo) {
        return misionFactory.crearConstancia(cantidadTiempo, unidadTiempo);
    }

    @Transactional
    public Mision crearMision(UUID idAdmin, String nomMision, String descripcion, String nomInsignia,
                              ReglaConstancia constancia, String atributo,
                              Operacion operacion) {
        AtributoImpacto atributoImpacto = misionFactory.crearAtributoImpacto(atributo);

        Mision mision = misionFactory.crearMision(
                idAdmin, nomMision, descripcion, nomInsignia,
                constancia,
                atributoImpacto,
                operacion
        );

        return repositorio.save(mision);
    }

    @Transactional
    public Mision actualizarMision(Mision misionActual, Mision misionModificada) {
        // Actualizar nombre de misión
        if (misionModificada.getNombreMision() != null) {
            misionActual.setNombreMision(misionModificada.getNombreMision());
        }

        // Actualizar descripción
        if (misionModificada.getDescripcion() != null) {
            misionActual.setDescripcion(misionModificada.getDescripcion());
        }

        // Actualizar insignia objetivo
        if (misionModificada.getInsigniaObjetivo() != null) {
            Insignia insigniaActualizada = new Insignia(
                    misionModificada.getInsigniaObjetivo().getNombre(),
                    misionModificada.getDescripcion() != null ? misionModificada.getDescripcion() : misionActual.getDescripcion()
            );
            misionActual.setInsigniaObjetivo(insigniaActualizada);
        }

        // Actualizar regla de progreso (constancia y operación)
        if (misionModificada.getReglaDeProgreso() != null) {
            misionActual.setReglaDeProgreso(misionModificada.getReglaDeProgreso());
        }

        return repositorio.save(misionActual);
    }
}