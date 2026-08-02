package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.CategoriaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.SecuenciaCategoriasDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorMision;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {
    private final GestorCategoria gestorCategoria;
    private final GestorMision gestorMisiones;

    public AdminService(GestorCategoria gestorCategoria,
                        GestorMision gestorMision){
        this.gestorCategoria = gestorCategoria;
        this.gestorMisiones = gestorMision;
    }

    public SecuenciaCategoriasDTO agregarCategoria(CategoriaDTO dto){
        List<Mision> misiones = gestorMisiones.conseguirMisiones(dto.getMisiones());

        Categoria categoria = new Categoria(
                dto.getNombre(),
                dto.getPosicionSecuencia(),
                misiones
        );

        return gestorCategoria.crearCategoria(categoria);
    }

    public SecuenciaCategoriasDTO eliminarCategoria(UUID id){
        return gestorCategoria.eliminarCategoria(id);
    }

    public CategoriaDTO actualizarCategoria(UUID id, CategoriaDTO dto){
        List<Mision> misiones = gestorMisiones.conseguirMisiones(dto.getMisiones());

        Categoria categoria = new Categoria(
                dto.getNombre(),
                dto.getPosicionSecuencia(),
                misiones
        );

        categoria.setIdCategoria(id);

        Categoria actualizada = gestorCategoria.actualizarCategoria(categoria);

        return actualizada != null? this.categoriaToDTO(actualizada) : null;
    }

    public CategoriaDTO categoriaToDTO(Categoria actualizada){
        List<String> nomMisiones = actualizada.getMisiones().stream()
                .map(Mision::getNombreMision).toList();

        return new CategoriaDTO(
                actualizada.getNombre(),
                actualizada.getPosicionSecuencia(),
                nomMisiones
        );
    }
}
