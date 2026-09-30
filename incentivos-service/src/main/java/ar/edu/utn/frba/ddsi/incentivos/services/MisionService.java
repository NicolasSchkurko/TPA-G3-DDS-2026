package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.controllers.request.MisionFiltroRequest;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.ConstanciaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.MisionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.OperacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.ReglaDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.SincronizacionPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.ValidadorAdmin;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioMisiones;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MisionService {
  private final RepositorioMisiones repoMisiones;
  private final MisionFactory misionFactory;
  private final SincronizacionPerfiles gestorSincronizacion;
  private final ValidadorAdmin validadorAdmin;

  public MisionService(RepositorioMisiones repoMisiones,
                       MisionFactory misionFactory,
                       SincronizacionPerfiles gestorSincronizacion,
                       ValidadorAdmin validadorAdmin) {
    this.repoMisiones = repoMisiones;
    this.misionFactory = misionFactory;
    this.gestorSincronizacion = gestorSincronizacion;
    this.validadorAdmin = validadorAdmin;
  }

  public Page<MisionDTO> obtenerMisiones(MisionFiltroRequest filtros, Pageable pageable) {
    return repoMisiones.obtenerTodas(
        filtros.nombreMision(),
        filtros.insigniaObjetivo(),
        filtros.atributo(),
        pageable
    ).map(MisionDTO::desdeEntidad);
  }

  public MisionDTO obtenerMisionPorId(UUID id) {
    Mision mision = repoMisiones.obtenerPorId(id);
    return MisionDTO.desdeEntidad(mision);
  }

  @Transactional
  public MisionDTO crearMision(UUID idAdmin, MisionDTO dto) {
    validadorAdmin.verificarPermisos(idAdmin);
    Mision mision = construirMision(idAdmin, dto);
    repoMisiones.save(mision);
    return MisionDTO.desdeEntidad(mision);
  }

  @Transactional
  public MisionDTO actualizarMision(UUID idAdmin, UUID idMision, MisionDTO dto) {
    validadorAdmin.verificarPermisos(idAdmin);

    return repoMisiones.findById(idMision).map(misionActual -> {
                         Mision misionModificada = construirMision(idAdmin, dto);

                         misionActual.actualizar(misionModificada);
                         Mision actualizada = repoMisiones.save(misionActual);
                         gestorSincronizacion.reiniciarProgresoDeMision(actualizada.getIdMision());

                         return actualizada;
                       })
                       .map(MisionDTO::desdeEntidad)
                       .orElse(null);
  }

  @Transactional
  public void eliminarMision(UUID idAdmin, UUID idMision) {
    validadorAdmin.verificarPermisos(idAdmin);
    repoMisiones.eliminarMision(idMision);
  }

  private Mision construirMision(UUID idAdmin, MisionDTO dto) {
    ReglaDTO regla = dto.getRegla();
    ConstanciaDTO constancia = regla.getConstancia();
    OperacionDTO operacion = regla.getOperacion();

    return misionFactory.crearMision(
        idAdmin,
        dto.getNombreMision(),
        dto.getDescripcion(),
        dto.getInsigniaObjetivo(),
        constancia != null ? constancia.getCantidad() : null,
        constancia != null ? constancia.getUnidadTiempo() : null,
        regla.getAtributo(),
        operacion.getTipoOperacion(),
        operacion.getProgresoObjetivo(),
        operacion.getCantidad(),
        operacion.getValorEsperado()
    );
  }
}