package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.ValoresDistintos;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorMision;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // ========== CATEGORÍAS - GET ==========
    public List<CategoriaDTO> obtenerCategorias(UUID idAdmin) {
        verificarPermisos(idAdmin);

        //TODO: esto deberia preguntarselo al repo directo o a una interface del repo (opcional, mejor)
        List<Categoria> categorias = gestorCategoria.obtenerTodas();
        return categorias.stream()
                        .map(this::categoriaToDTO)
                        .toList();
    }

    public CategoriaDTO obtenerCategoriaPorId(UUID idAdmin, UUID id) {
        verificarPermisos(idAdmin);
        //TODO: esto deberia preguntarselo al repo directo o a una interface del repo (opcional, mejor)
        Categoria categoria = gestorCategoria.obtenerPorId(id);
        return categoria != null ? categoriaToDTO(categoria) : null;
    }

    // ========== CATEGORÍAS - CREATE ==========
    @Transactional
    public CategoriaDTO agregarCategoria(UUID idAdmin, CategoriaDTO dto) {
        verificarPermisos(idAdmin);

        //TODO: esto esta raro. Quien deberia tener la resp de buscar las misiones desde el dto
        // Quiza haria una funcion private  del service que busque misiones a partir de uuid
        // mas que nada pq no creo que gestor misiones debaconocer un dto
        // no dije nada es una lista de uuid entonces que le pegue al repo
        // Maten a los gestores, domingo rojo
        List<Mision> misiones = gestorMisiones.conseguirMisiones(dto.getMisiones());

        Categoria categoria = new Categoria(
            dto.getNombre(),
            idAdmin,
            dto.getPosicionSecuencia(),
            misiones
        );


        // No hace falta aclarar nada aca, me ahorro ponerlo
        Categoria categoriaCreada = gestorCategoria.crearCategoria(categoria);
        return categoriaCreada != null ? categoriaToDTO(categoriaCreada) : null;
    }

    // ========== CATEGORÍAS - UPDATE ==========
    @Transactional
    public CategoriaDTO actualizarCategoria(UUID idAdmin, UUID id, CategoriaDTO dto) {
        verificarPermisos(idAdmin);
        // TODO idem arriba
        List<Mision> misiones = gestorMisiones.conseguirMisiones(dto.getMisiones());

        Categoria categoria = new Categoria(
            dto.getNombre(),
            idAdmin,
            dto.getPosicionSecuencia(),
            misiones
        );
        categoria.setIdCategoria(id);

        Categoria actualizada = gestorCategoria.actualizarCategoria(categoria);
        return actualizada != null ? categoriaToDTO(actualizada) : null;
    }

    // ========== CATEGORÍAS - DELETE ==========
    @Transactional
    public List<CategoriaDTO> eliminarCategoria(UUID idAdmin, UUID id) {
        verificarPermisos(idAdmin);
        // TODO: Ya creo que a nadie le sorprende
        List<Categoria> categorias = gestorCategoria.eliminarCategoria(id);
        return categorias.stream()
                        .map(this::categoriaToDTO)
                        .toList();
    }

    // ========== MISIONES - GET ==========
    public List<MisionDTO> obtenerMisiones(UUID idAdmin) {
        verificarPermisos(idAdmin);

        // TODO: ITS REPO TIME
        List<Mision> misiones = gestorMisiones.obtenerTodas();
        return misiones.stream()
                      .map(this::misionToDTO)
                      .toList();
    }

    public MisionDTO obtenerMisionPorId(UUID idAdmin, UUID id) {
        verificarPermisos(idAdmin);

        // TODO: miren al final del .java
        Mision mision = gestorMisiones.obtenerPorId(id);
        return mision != null ? misionToDTO(mision) : null;
    }

    // ========== MISIONES - CREATE ==========
    @Transactional
    public MisionDTO crearMision(UUID idAdmin, MisionDTO nuevaMision) {
        verificarPermisos(idAdmin);

        ReglaDTO reglaDTO = nuevaMision.getRegla();
        ConstanciaDTO constanciaDTO = reglaDTO.getConstancia();
        OperacionDTO operacionDTO = reglaDTO.getOperacion();
        String atributo = reglaDTO.getAtributo();


        // TODO: Si te digo te sorprendo dsp lo voy arreglando pero bue dejo constancia sino cuelgo
        Mision m = gestorMisiones.crearMision(
            idAdmin,
            nuevaMision.getNombreMision(),
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
        return misionToDTO(m);
    }

    // ========== MISIONES - UPDATE ==========
    @Transactional
    public MisionDTO actualizarMision(UUID idAdmin, UUID idMision, MisionDTO dto) {
        verificarPermisos(idAdmin);

        ReglaDTO reglaDTO = dto.getRegla();
        ConstanciaDTO constanciaDTO = reglaDTO.getConstancia();
        OperacionDTO operacionDTO = reglaDTO.getOperacion();
        String atributo = reglaDTO.getAtributo();
        // TODO: Skibidi dub dub dub yes yes
        Mision mision = gestorMisiones.crearMision(
            idAdmin,
            dto.getNombreMision(),
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

        Mision actualizada = gestorMisiones.actualizarMision(mision);
        return actualizada != null ? misionToDTO(actualizada) : null;
    }

    // ========== MISIONES - DELETE ==========
    @Transactional
    public MisionDTO eliminarMision(UUID idAdmin, UUID idMision) {
        verificarPermisos(idAdmin);
        // TODO: Genio!
        Mision mision = gestorMisiones.eliminarMision(idMision);
        return misionToDTO(mision);
    }

    // ========== CONVERTIDORES ==========
    private CategoriaDTO categoriaToDTO(Categoria categoria) {
        List<UUID> idMisiones = categoria.getCategoriaMisiones().stream()
                                         .map(cm -> cm.getMision().getIdMision())
                                         .toList();

        return new CategoriaDTO(
            categoria.getNombre(),
            categoria.getPosicionSecuencia(),
            idMisiones
        );
    }

    private MisionDTO misionToDTO(Mision mision) {
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


/* efectivamente repo ya estoy sucumbiendo a la locura abstenerse a las consecuencias
                                              =*#%###*#
                                            #.         **------+#*#
                                           # # ###      *+----------+*     +#*
                                          # #######      #---------*           #
                                         #  #####-      %----------#            #
                                        :+             #*---------+*      ####   #
                                        ##           :#------------#      ###:   #
                                       #--#       ##*---------------#     :####  #
                                      #-------+=---------------------+#          #
                                     =+-------*#*+==============+##*----#+       #
                                     #----##==========================*#---##*=##
                                     *-+#=================================#-----*
                                    +=#=====================================#=--#
                                    ##========================================#-#
                                    #==========================================#*
                                    #==========================================*#
                                     #=========================================+*
                                    :+#*++=====================================#
                                 *--------------+**####+======================#
                                 #-------------------------+#**#+===========*#
                                 #---------------------------------=###+=+#
                                #-----------------------------------------##*
                                #---------------------------------------------*#
                            ##+----=+*######*+---------------------------------#
                       *#*--#---------------------+###*+----------------------+
                   #*+-----++-------------------------------+##*--------------#
               ##----------#---------------------------------------=###-------+
             #-------------#---------------------------------------------=*#+#
               .##*-------*---------------------------------------------------*#
                      ###**----------------------------------------------------*:
                         #*=======+*#######------------------------------------#-*+
                           ##+====#*#####+==-===**###*-------------------------*---*#
                               ####--------**#============###*----------------#-------#
                                  #-----------#==================###+---------*---------#
                                  #----------####*=====#------*##======*##---#------------#
                                  #----------#        *#----------=#=======+####***+=------#
                                  #---------#          #----------+##########
                                   *-------#           #----------#
                                   #------#            #---------#
                                   #-----#             #--------#
                                   #----#              :=------*
                                   *=--#                *-----*
                                    #*                  *----#
                                                        #---#
                                                        +*+#
*/

