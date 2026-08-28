package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioAdministradores;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Gestor (Servicio) para manejar la lógica de negocio relacionada con los Administradores.
 */
@Service
public class GestorAdministradores {

  private RepositorioAdministradores repositorio;

  public GestorAdministradores(RepositorioAdministradores repositorio) {
    this.repositorio = repositorio;
  }

  public List<Administrador> listarTodosLosAdministradores() {
    return repositorio.obtenerTodos();
  }

}