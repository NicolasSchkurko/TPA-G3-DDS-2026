package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import lombok.Setter;
import lombok.Getter;

@Getter
@Setter

public class RepositorioPerfiles {
    private static RepositorioPerfiles instanciaUnica;

    private final List<Perfil> perfiles;

    private RepositorioPerfiles() {
        this.perfiles = new ArrayList<>();
    }

    public static RepositorioPerfiles getInstance() {
        if (instanciaUnica == null) {
            instanciaUnica = new RepositorioPerfiles();
        }
        return instanciaUnica;
    }

    public void agregarPerfil(Perfil perfil) {
        if (!perfiles.contains(perfil)) {
            perfiles.add(perfil);
        }
    }

    public void eliminarPerfil(Perfil perfil) {
        if(perfiles.contains(perfil)){
            perfiles.remove(perfil);
        }
    }

    public List<Perfil> listarTodos() {
        return List.copyOf(perfiles);
    }

    public Perfil buscarPorNombreUsuario(String nombreUsuario) {
        if (perfiles.isEmpty() || nombreUsuario == null) {
            return null;
        }

        String busqueda = nombreUsuario.trim();

        Predicate<Perfil> nombrePredicate = predicatePorNombreUsuario(busqueda);

        return findBy(nombrePredicate).orElse(null);
    }

    private Optional<Perfil> findBy(Predicate<Perfil> predicate) {
        return perfiles.stream()
                .filter(predicate)
                .findFirst();
    }
    
    private Predicate<Perfil> predicatePorNombreUsuario(String busqueda) {
        return perfil -> matchesNombreUsuario(perfil, busqueda);
    }

    private boolean matchesNombreUsuario(Perfil perfil, String busqueda) {
        String nombrePerfil = perfil.getNombreUsuario();
        return nombrePerfil != null && nombrePerfil.trim().equalsIgnoreCase(busqueda);
    }
}
