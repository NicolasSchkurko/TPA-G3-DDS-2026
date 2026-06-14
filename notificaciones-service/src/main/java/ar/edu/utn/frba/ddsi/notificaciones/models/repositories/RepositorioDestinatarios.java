package ar.edu.utn.frba.ddsi.notificaciones.models.repositories;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Destinatario.Destinatario;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.Optional;

public class RepositorioDestinatarios {
    private static RepositorioDestinatarios instanciaUnica;

    private static List<Destinatario> destinatarios;

    private RepositorioDestinatarios() {
        destinatarios = new ArrayList<>();
    }

    public static RepositorioDestinatarios getInstance() {
        if (instanciaUnica == null) {
            instanciaUnica = new RepositorioDestinatarios();
        }
        return instanciaUnica;
    }

    public void agregarPersona(Destinatario destinatario) {
        if(!destinatarios.contains(destinatario)){
            destinatarios.add(destinatario);
        }
    }

    public void eliminarPersona(Destinatario destinatario) {
        if(destinatarios.contains(destinatario)){
            destinatarios.remove(destinatario);
        }
    }

    public Destinatario getPersonaPorNumeroId(int numeroId){
        Destinatario destinatario= destinatarios.get(numeroId);
        return destinatario;
    }

    public static List<Destinatario> getPersonas() {
        return List.copyOf(destinatarios);
    }

    public static Destinatario getPersonaPorNombreCompleto(String nombreBuscado) {

        if (destinatarios == null || destinatarios.isEmpty() || nombreBuscado == null) {
            return null;
        }

        String busqueda = nombreBuscado.trim();

        Predicate<Destinatario> namePredicate = predicatePorNombre(busqueda);

        return findBy(namePredicate).orElse(null);
    }

    /* Helper genérico que permite buscar por cualquier predicado */
    private static Optional<Destinatario> findBy(Predicate<Destinatario> predicate) {
        return destinatarios.stream()
                .filter(predicate)
                .findFirst();
    }

    /* Predicado compuesto para buscar por "nombre" (darNombre, razonSocial, nombre completo) */
    private static Predicate<Destinatario> predicatePorNombre(String busqueda) {
        return persona -> matchesDarNombre(persona, busqueda);
    }

    private static boolean matchesDarNombre(Destinatario destinatario, String busqueda) {
        try {
            String nombrePersona = destinatario.getNombre();
            return nombrePersona != null && nombrePersona.trim().equalsIgnoreCase(busqueda);
        } catch (Exception ignored) {
            return false;
        }
    }


}
