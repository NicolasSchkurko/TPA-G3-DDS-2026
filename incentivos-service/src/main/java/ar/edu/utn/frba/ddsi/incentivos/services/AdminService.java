package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.ValoresDistintos;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
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
    private final DonacionClient donacionClient;

    public AdminService(GestorCategoria gestorCategoria,
                        GestorMision gestorMision,
                        DonacionClient donacionClient) {
        this.gestorCategoria = gestorCategoria;
        this.gestorMisiones = gestorMision;
        this.donacionClient = donacionClient;
        gestorCategoria.inicializarCategoriasBase();
    }

    private void verificarPermisos(UUID idAdmin) {
        if (!donacionClient.verificarAdmin(idAdmin)) {
            throw new SecurityException("El usuario no tiene permisos de administrador o no existe.");
        }
    }

    public List<CategoriaDTO> agregarCategoria(UUID idAdmin, CategoriaDTO dto) {
        verificarPermisos(idAdmin);

        List<Mision> misiones = gestorMisiones.conseguirMisiones(dto.getMisiones());

        Categoria categoria = new Categoria(
            dto.getNombre(),
            idAdmin,
            dto.getPosicionSecuencia(),
            misiones
        );

        List<Categoria> categorias = gestorCategoria.crearCategoria(categoria);

        List<CategoriaDTO> categoriasDTO = new ArrayList<>();
        for(Categoria x : categorias){
            List<UUID> idMisiones = x.getCategoriaMisiones().stream() // Corregido: getCategoriaMisiones()
                                     .map(cm -> cm.getMision().getIdMision()).toList();

            CategoriaDTO cat = new CategoriaDTO(
                x.getNombre(),
                x.getPosicionSecuencia(),
                idMisiones
            );
            categoriasDTO.add(cat);
        }

        return categoriasDTO;
    }

    public List<CategoriaDTO> eliminarCategoria(UUID idAdmin, UUID id) {
        verificarPermisos(idAdmin);

        List<Categoria> categorias = gestorCategoria.eliminarCategoria(id);

        List<CategoriaDTO> categoriasDTO = new ArrayList<>();
        for(Categoria x : categorias){
            List<UUID> idMisiones = x.getCategoriaMisiones().stream() // Corregido: getCategoriaMisiones()
                                     .map(cm -> cm.getMision().getIdMision()).toList();

            CategoriaDTO cat = new CategoriaDTO(
                x.getNombre(),
                x.getPosicionSecuencia(),
                idMisiones
            );
            categoriasDTO.add(cat);
        }

        return categoriasDTO;
    }

    public CategoriaDTO actualizarCategoria(UUID idAdmin, UUID id, CategoriaDTO dto) {
        verificarPermisos(idAdmin);

        List<Mision> misiones = gestorMisiones.conseguirMisiones(dto.getMisiones());

        Categoria categoria = new Categoria(
            dto.getNombre(),
            idAdmin,
            dto.getPosicionSecuencia(),
            misiones
        );

        categoria.setIdCategoria(id);

        Categoria actualizada = gestorCategoria.actualizarCategoria(categoria);

        return actualizada != null ? this.categoriaToDTO(actualizada) : null;
    }

    public MisionDTO crearMision(UUID idAdmin, MisionDTO nuevaMision) {
        verificarPermisos(idAdmin);

        ReglaDTO reglaDTO = nuevaMision.getRegla();
        ConstanciaDTO constanciaDTO = reglaDTO.getConstancia();
        OperacionDTO operacionDTO = reglaDTO.getOperacion();
        String atributo = reglaDTO.getAtributo();

        Mision m = gestorMisiones.crearMision(nuevaMision.getNombreMision(),
                                              nuevaMision.getDescripcion(),
                                              nuevaMision.getInsigniaObjetivo(),
                                              gestorMisiones.conseguirConstancia(
                                                  constanciaDTO.getCantidad(),
                                                  constanciaDTO.getUnidadTiempo()
                                              ),
                                              atributo,
                                              gestorMisiones.conseguirOperacion(
                                                  operacionDTO.getTipoOperacion(),
                                                  operacionDTO.getProgresoObjetivo(),
                                                  operacionDTO.getCantidad(),
                                                  operacionDTO.getValorEsperado()
                                              )
        );
        m.setIdAdmin(idAdmin); // Asignando el admin

        return misionToDTO(m);
    }

    public MisionDTO eliminarMision(UUID idAdmin, UUID idMision) {
        verificarPermisos(idAdmin);

        Mision mision = gestorMisiones.eliminarMision(idMision);
        return misionToDTO(mision);
    }

    public MisionDTO actualizarMision(UUID idAdmin, UUID idMision, MisionDTO dto) {
        verificarPermisos(idAdmin);

        ReglaDTO reglaDTO = dto.getRegla();
        ConstanciaDTO constanciaDTO = reglaDTO.getConstancia();
        OperacionDTO operacionDTO = reglaDTO.getOperacion();
        String atributo = reglaDTO.getAtributo();

        Mision mision = gestorMisiones.crearMision(dto.getNombreMision(),
                                                   dto.getDescripcion(),
                                                   dto.getInsigniaObjetivo(),
                                                   gestorMisiones.conseguirConstancia(
                                                       constanciaDTO.getCantidad(),
                                                       constanciaDTO.getUnidadTiempo()
                                                   ),
                                                   atributo,
                                                   gestorMisiones.conseguirOperacion(
                                                       operacionDTO.getTipoOperacion(),
                                                       operacionDTO.getProgresoObjetivo(),
                                                       operacionDTO.getCantidad(),
                                                       operacionDTO.getValorEsperado()
                                                   )
        );

        mision.setIdMision(idMision);
        mision.setIdAdmin(idAdmin);

        Mision actualizada = gestorMisiones.actualizarMision(mision);

        return actualizada != null ? this.misionToDTO(actualizada) : null;
    }

    public CategoriaDTO categoriaToDTO(Categoria actualizada) {
        List<UUID> idMisiones = actualizada.getCategoriaMisiones().stream() // Corregido
                                           .map(cm -> cm.getMision().getIdMision()).toList();

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
            mision.getDescripcion(),
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