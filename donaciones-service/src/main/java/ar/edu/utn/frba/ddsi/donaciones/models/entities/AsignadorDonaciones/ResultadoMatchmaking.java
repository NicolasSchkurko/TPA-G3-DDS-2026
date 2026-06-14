package ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones;


import java.util.List;
import java.util.Map;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResultadoMatchmaking {

  // Lista a mostrar si el filtro automático funcionó (Intersección)
  private List<EntidadBeneficiaria> opcionesCoincidentes;

  // Mapa a mostrar si el filtro automático falló (Listas separadas por algoritmo)
  private Map<String, List<EntidadBeneficiaria>> resultadosCompletos;

  // Flag para la Interfaz de Usuario
  private boolean filtroAutomaticoExitoso;

  // Constructor para cuando hay coincidencias
  public ResultadoMatchmaking(List<EntidadBeneficiaria> opcionesCoincidentes, boolean filtroAutomaticoExitoso) {
    this.opcionesCoincidentes = opcionesCoincidentes;
    this.resultadosCompletos = null;
    this.filtroAutomaticoExitoso = filtroAutomaticoExitoso;
  }

  // Constructor para cuando no hay coincidencias y se muestran todas las listas
  public ResultadoMatchmaking(Map<String, List<EntidadBeneficiaria>> resultadosCompletos, boolean filtroAutomaticoExitoso) {
    this.opcionesCoincidentes = null;
    this.resultadosCompletos = resultadosCompletos;
    this.filtroAutomaticoExitoso = filtroAutomaticoExitoso;
  }
}