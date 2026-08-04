package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.ValoresDistintos;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorMision;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AdminService {
    private final GestorCategoria gestorCategoria;
    private final GestorMision gestorMisiones;

    public AdminService(GestorCategoria gestorCategoria,
                        GestorMision gestorMision){
        this.gestorCategoria = gestorCategoria;
        this.gestorMisiones = gestorMision;
    }

    public SecuenciaCategoriasDTO agregarCategoria(CategoriaDTO dto){
        List<Mision> misiones = gestorMisiones.conseguirMisiones(dto.getMisiones());

        Categoria categoria = new Categoria(
                dto.getNombre(),
                dto.getPosicionSecuencia(),
                misiones
        );

        List<Categoria> categorias = gestorCategoria.crearCategoria(categoria);

        List<CategoriaDTO> categoriasDTO = new ArrayList<>();
        for(Categoria x : categorias){
            List<UUID> idMisiones = x.getMisiones().stream()
                    .map(Mision::getIdMision).toList();

            CategoriaDTO cat = new CategoriaDTO(
                    x.getNombre(),
                    x.getPosicionSecuencia(),
                    idMisiones
            );

            categoriasDTO.add(cat);
        }

        return new SecuenciaCategoriasDTO(categoriasDTO);
    }

    public SecuenciaCategoriasDTO eliminarCategoria(UUID id){
        List<Categoria> categorias = gestorCategoria.eliminarCategoria(id);

        List<CategoriaDTO> categoriasDTO = new ArrayList<>();
        for(Categoria x : categorias){
            List<UUID> idMisiones = x.getMisiones().stream()
                    .map(Mision::getIdMision).toList();

            CategoriaDTO cat = new CategoriaDTO(
                    x.getNombre(),
                    x.getPosicionSecuencia(),
                    idMisiones
            );

            categoriasDTO.add(cat);
        }

        return new SecuenciaCategoriasDTO(categoriasDTO);
    }

    public CategoriaDTO actualizarCategoria(UUID id, CategoriaDTO dto){
        List<Mision> misiones = gestorMisiones.conseguirMisiones(dto.getMisiones());

        Categoria categoria = new Categoria(
                dto.getNombre(),
                dto.getPosicionSecuencia(),
                misiones
        );

        categoria.setIdCategoria(id);

        Categoria actualizada = gestorCategoria.actualizarCategoria(categoria);

        return actualizada != null? this.categoriaToDTO(actualizada) : null;
    }

    public MisionDTO crearMision(MisionDTO nuevaMision){
        Mision m = gestorMisiones.crearMision(nuevaMision.getNombreMision(),
                nuevaMision.getInsigniaObjetivo(),
                nuevaMision.getRegla().getConstancia().getCantidad(),
                nuevaMision.getRegla().getConstancia().getUnidadTiempo(),
                nuevaMision.getRegla().getAtributo(),
                nuevaMision.getRegla().getOperacion().getTipoOperacion(),
                nuevaMision.getRegla().getOperacion().getProgresoObjetivo(),
                nuevaMision.getRegla().getOperacion().getCantidad(),
                nuevaMision.getRegla().getOperacion().getValorEsperado());

        return misionToDTO(m);
    }

    public MisionDTO eliminarMision(UUID idMision){
        Mision mision = gestorMisiones.eliminarMision(idMision);
        return misionToDTO(mision);
    }

    public MisionDTO actualizarMision(UUID idMision, MisionDTO mision){

    }

    public CategoriaDTO categoriaToDTO(Categoria actualizada){
        List<UUID> idMisiones = actualizada.getMisiones().stream()
                .map(Mision::getIdMision).toList();

        return new CategoriaDTO(
                actualizada.getNombre(),
                actualizada.getPosicionSecuencia(),
                idMisiones
        );
    }

    public MisionDTO misionToDTO(Mision mision) {
        if (mision == null) {
            return null;
        }

        ReglaConstancia reglaConstancia = mision.getReglaDeProgreso().getConstancia();
        ConstanciaDTO constancia = reglaConstancia == null
                ? null
                : new ConstanciaDTO(
                        reglaConstancia.getCantidad(),
                        reglaConstancia.getUnidadTiempo().toString()
                );

        return new MisionDTO(
                mision.getNombreMision(),
                mision.getInsigniaObjetivo().getNombre(),
                constancia,
                mision.getReglaDeProgreso().getAtributo().name(),
                operacionToDTO(mision.getReglaDeProgreso().getOperacion())
        );
    }

    private OperacionDTO operacionToDTO(Operacion operacion) {
        if (operacion instanceof CantidadCoincidencias coincidencias) {
            return new OperacionDTO(
                    "COINCIDENCIAS",
                    coincidencias.getProgresoObjetivo(),
                    String.valueOf(coincidencias.getValorEsperado()),
                    null
            );
        }

        if (operacion instanceof ValoresDistintos distintos) {
            return new OperacionDTO(
                    "VALORES_DISTINTOS",
                    distintos.getProgresoObjetivo(),
                    null,
                    distintos.getCantValoresDistintos()
            );
        }

        if (operacion instanceof SuperaCantidad superaCantidad) {
            return new OperacionDTO(
                    "SUPERA_CANTIDAD",
                    superaCantidad.getProgresoObjetivo(),
                    null,
                    superaCantidad.getCantidadEsperada()
            );
        }

        throw new IllegalArgumentException(
                "Tipo de operación no soportado: " + operacion.getClass().getSimpleName()
        );
    }
}
