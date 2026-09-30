package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ValidadorAdmin {
  private final DonacionClient donacionClient;

  public ValidadorAdmin(DonacionClient donacionClient) {
    this.donacionClient = donacionClient;
  }

  public void verificarPermisos(UUID idAdmin) {
    if (!donacionClient.verificarAdmin(idAdmin)) {
      throw new SecurityException("El usuario no tiene permisos de administrador o no existe.");
    }
  }
}