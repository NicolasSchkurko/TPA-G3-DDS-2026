package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioMisiones;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GestorMision {
    private final RepositorioMisiones repositorio;
    private final MisionFactory misionFactory;

    public GestorMision(RepositorioMisiones repositorio, MisionFactory misionFactory) {
        this.repositorio = repositorio;
        this.misionFactory = misionFactory;
    }

    public List<Mision> conseguirMisiones(List<UUID> idMisiones) {
        // JPA maneja esto de forma nativa con una única consulta SQL (WHERE id IN (...))
        return repositorio.findAllById(idMisiones);
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
    public Mision crearMision(String nomMision, String descripcion, String nomInsignia,
                              ReglaConstancia constancia, String atributo,
                              Operacion operacion) {
        AtributoImpacto atributoImpacto = misionFactory.crearAtributoImpacto(atributo);

        Mision mision = misionFactory.crearMision(
            nomMision, descripcion, nomInsignia,
            constancia,
            atributoImpacto,
            operacion
        );

        return repositorio.save(mision);
    }

    @Transactional
    public Mision eliminarMision(UUID idMision) {
        Mision m = repositorio.findById(idMision).orElse(null);
        if (m != null) {
            repositorio.delete(m);
        }
        return m;
    }

    @Transactional
    public Mision actualizarMision(Mision mision) {
        if (mision.getIdMision() == null) return null;

        Mision misionActual = repositorio.findById(mision.getIdMision()).orElse(null);
        if (misionActual == null) return null;

        if (mision.getNombreMision() != null) {
            misionActual.setNombreMision(mision.getNombreMision());
            misionActual.getInsigniaObjetivo().setDescripcion(mision.getNombreMision());
        }

        if (mision.getInsigniaObjetivo() != null) {
            misionActual.setInsigniaObjetivo(
                new Insignia(
                    mision.getInsigniaObjetivo().getNombre(),
                    misionActual.getNombreMision()
                )
            );
        }

        return repositorio.save(misionActual);
    }
}