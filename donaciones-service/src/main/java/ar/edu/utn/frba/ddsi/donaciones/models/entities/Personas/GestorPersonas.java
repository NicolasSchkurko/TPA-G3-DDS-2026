package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

import java.util.ArrayList;
import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.AsignadorDonaciones;
import lombok.Setter;
import lombok.Getter;

@Getter
@Setter

public class GestorPersonas {
    private static GestorPersonas instanciaUnica;

    private static List<PersonaDonante> personas;

    private GestorPersonas() {
        this.personas = new ArrayList<>();
    }

    public static GestorPersonas getInstance() {
        if (instanciaUnica == null) {
            instanciaUnica = new GestorPersonas();
        }
        return instanciaUnica;
    }

    public void agregarPersona(PersonaDonante persona) {
        if(!personas.contains(persona)){
            this.personas.add(persona);
        }
    }

    public void eliminarPersona(PersonaDonante persona) {
        if(personas.contains(persona)){
            this.personas.remove(persona);
        }
    }

    public static PersonaDonante personaExiste(String nombreBuscado){
        // Asegurarnos de que la lista esté inicializada
        if (personas == null) {
            getInstance();
        }

        if (personas == null || personas.isEmpty() || nombreBuscado == null) {
            return null;
        }

        String busqueda = nombreBuscado.trim();

        for (PersonaDonante persona : personas) {
            // 1) Comparo con lo que devuelve darNombre() (ya definido en cada subclass)
            String darNombre = null;
            try {
                darNombre = persona.darNombre();
            } catch (Exception ignored) { }

            if (darNombre != null && darNombre.trim().equalsIgnoreCase(busqueda)) {
                return persona;
            }

            // 2) Si es PersonaJuridica, comparar con razonSocial
            if (persona instanceof PersonaJuridica) {
                String razon = ((PersonaJuridica) persona).getRazonSocial();
                if (razon != null && razon.trim().equalsIgnoreCase(busqueda)) {
                    return persona;
                }
            }

            // 3) Si es PersonaHumana, además comparar nombre + apellido completo
            if (persona instanceof PersonaHumana) {
                PersonaHumana ph = (PersonaHumana) persona;
                if (ph.getPersona() != null) {
                    String nombre = ph.getPersona().getNombre();
                    String apellido = ph.getPersona().getApellido();
                    String nombreCompleto = (nombre == null ? "" : nombre.trim())
                        + (apellido == null || apellido.isBlank() ? "" : " " + apellido.trim());
                    if (!nombreCompleto.isBlank() && nombreCompleto.equalsIgnoreCase(busqueda)) {
                        return persona;
                    }
                }
            }
        }

        return null;
    }
}

