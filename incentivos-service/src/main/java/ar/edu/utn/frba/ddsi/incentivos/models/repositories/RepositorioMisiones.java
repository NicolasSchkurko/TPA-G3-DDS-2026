package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class RepositorioMisiones {
    private final List<Mision> misiones;
    private MisionFactory misionFactory;
    public RepositorioMisiones(MisionFactory misionFactory) {
        this.misiones = new ArrayList<>();
        this.misionFactory = misionFactory;
    }

    public void agregarMision(Mision mision) {
        if (!misiones.contains(mision)) {
            misiones.add(mision);
        }
    }

    public void eliminarMision(Mision mision) {
        misiones.remove(mision);
    }

    public Mision actualizar(Mision misionModificada) {
        if (misionModificada == null) {
            return null;
        }

        Mision existente = this.buscarPorId(misionModificada.getIdMision());

        if (existente != null) {
            int index = misiones.indexOf(existente);
            if (index >= 0) {
                misiones.set(index, existente);
            }
            return existente;
        }

        return null;
    }

    public Mision buscarPorId(UUID id) {
        if (id == null || misiones.isEmpty()) return null;

        return misiones.stream()
                .filter(m -> id.equals(m.getIdMision()))
                .findFirst()
                .orElse(null);
    }

    public List<Mision> obtenerTodas() {
        return new ArrayList<>(this.misiones);
    }

    public void inicializarMisionesBase() {
        agregarMision( misionFactory.crearMision(
                        "Racha",
                        "realizar 1 donación durante 3 meses consecutivos",
                        "Usuario Constante",
                        misionFactory.crearConstancia(3, "months"),
                        misionFactory.crearAtributoImpacto("estado"),
                        misionFactory.crearOperacion("COINCIDENCIAS",1, null,
                                "ENTREGADA")
                )
        );

        agregarMision( misionFactory.crearMision(
                        "Completitud",
                        "realizar 5 donaciones de 3 categorías distintas",
                        "Usuario Variado",
                        null,
                        misionFactory.crearAtributoImpacto("categoria"),
                        misionFactory.crearOperacion("VALORES_DISTINTOS",5, 3,
                                null)
                )
        );

        agregarMision( misionFactory.crearMision(
                        "Habil Donador",
                        "1 donación que supere 3 cantidad de bienes",
                        "Usuario Generoso",
                        null,
                        misionFactory.crearAtributoImpacto("CANTIDAD_BIENES"),
                        misionFactory.crearOperacion("SUPERA_CANTIDAD",1, 3,
                                null)
                )
        );

        agregarMision( misionFactory.crearMision(
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
}
