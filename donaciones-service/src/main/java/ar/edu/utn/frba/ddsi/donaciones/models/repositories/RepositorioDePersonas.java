package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaJuridica;
import java.util.ArrayList;
import java.util.List;

import java.util.Optional;
import java.util.function.Predicate;
import lombok.Setter;
import lombok.Getter;

@Getter
@Setter

public class RepositorioDePersonas {
    private static RepositorioDePersonas instanciaUnica;

    private static List<PersonaDonante> personas;

    private RepositorioDePersonas() {
        this.personas = new ArrayList<>();
    }

    public static RepositorioDePersonas getInstance() {
        if (instanciaUnica == null) {
            instanciaUnica = new RepositorioDePersonas();
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

    public static List<PersonaDonante> getPersonas() {
        return List.copyOf(personas);
    }

    public static PersonaDonante getPersonaPorNombreCompleto(String nombreBuscado) {

        if (personas == null || personas.isEmpty() || nombreBuscado == null) {
            return null;
        }

        String busqueda = nombreBuscado.trim();

        Predicate<PersonaDonante> namePredicate = predicatePorNombre(busqueda);

        return findBy(namePredicate).orElse(null);
    }

    /* Helper genérico que permite buscar por cualquier predicado */
    private static Optional<PersonaDonante> findBy(Predicate<PersonaDonante> predicate) {
        return personas.stream()
                       .filter(predicate)
                       .findFirst();
    }

    /* Predicado compuesto para buscar por "nombre" (darNombre, razonSocial, nombre completo) */
    private static Predicate<PersonaDonante> predicatePorNombre(String busqueda) {
        return persona -> matchesDarNombre(persona, busqueda);
    }

    private static boolean matchesDarNombre(PersonaDonante persona, String busqueda) {
        try {
            String nombrePersona = persona.darNombre();
            return nombrePersona != null && nombrePersona.trim().equalsIgnoreCase(busqueda);
        } catch (Exception ignored) {
            return false;
        }
    }


}

