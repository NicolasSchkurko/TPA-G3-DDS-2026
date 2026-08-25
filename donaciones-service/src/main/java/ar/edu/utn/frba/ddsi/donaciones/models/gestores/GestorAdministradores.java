package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioAdministradores;
import java.util.List;
import java.util.UUID;

/**
 * Gestor (Servicio) para manejar la lógica de negocio relacionada con los Administradores.
 */
public class GestorAdministradores {

  private RepositorioAdministradores repositorio;

  public GestorAdministradores() {
    this.repositorio = new RepositorioAdministradores();
  }

  public void registrarAdministrador(Administrador nuevoAdmin) {
    try {
      repositorio.guardar(nuevoAdmin);
      System.out.println("Administrador registrado con éxito con ID: " + nuevoAdmin.getId());
    } catch (IllegalArgumentException e) {
      System.err.println("Error al registrar administrador: " + e.getMessage());
    }
  }

  public Administrador obtenerAdministrador(UUID id) {
    return repositorio.buscarPorId(id).orElse(null);
  }

  public List<Administrador> listarTodosLosAdministradores() {
    return repositorio.obtenerTodos();
  }

  public void modificarAdministrador(UUID idOriginal, Administrador datosNuevos) {
    try {
      repositorio.actualizar(idOriginal, datosNuevos);
      System.out.println("Administrador actualizado con éxito.");
    } catch (IllegalArgumentException e) {
      System.err.println("Error al modificar administrador: " + e.getMessage());
    }
  }

  public void darDeBajaAdministrador(UUID id) {
    repositorio.eliminarPorId(id);
    System.out.println("Administrador dado de baja (si existía).");
  }
}